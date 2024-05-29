package WaterWarriors;

import robocode.*;
import static robocode.util.Utils.normalRelativeAngle;
import robocode.util.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;  

public class WaterWarrior extends AdvancedRobot {

    // Pesos das caracteristicas para a previsão. Ou seja é a importancia que cada caracteristica possui na previsão.
    private double[] pesos = {0.0112, -0.0118, 0.0223, 0.0146, -0.0201}; 
    
    // É o valor que permite ajustar a posição da fronteira de decisão da função de regressão logísticad
    private double intercepto = 0.418; 
    
    private double margem = 60;

    private List<Double> R_direcao = new ArrayList<Double>();
    private List<Double> E_angulo = new ArrayList<Double>();
    private List<Double> E_distancia = new ArrayList<Double>();
    private List<Double> E_velocidade = new ArrayList<Double>();
    private List<Double> E_direcao = new ArrayList<Double>();
    
    private LogisticRegression LR;
	
    private double mapaLargura;
    private double mapaAltura;
    
    private double anguloAbsoluto;    
		
    public void run() {

        setColors(Color.BLUE, Color.ORANGE, Color.ORANGE);
	
        LR = new LogisticRegression(pesos, intercepto);
        
        mapaAltura = getBattleFieldHeight();
        mapaLargura = getBattleFieldWidth();
		
        ajustarConfiguracoes();

        while (true) {

            if (getRadarTurnRemaining() == 0.0) {
				setTurnRadarRight(360);
				setAhead(100);
			} 
			
			execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {   
        double direcao = getHeading();
        double angulo = e.getBearing();
        double distancia = e.getDistance();
        double velocidade = e.getVelocity();
        double e_direcao = e.getHeading();

        anguloAbsoluto = getHeadingRadians() + e.getBearingRadians();
        
        // Armazena as características coletadas
        R_direcao.add(direcao);
        E_distancia.add(distancia);
        E_angulo.add(angulo);
        E_velocidade.add(velocidade);
        E_direcao.add(e_direcao);

        // Calcula a velocidade média do disparo
		double velocidadeDisparo = 20 - 3 * potenciaTiro(distancia);
		
        // Calcula a velocidade lateral do inimigo
		double velocidadeInimigo = velocidade * Math.sin(e.getHeadingRadians() - anguloAbsoluto); 
		
        // Ajusta a mira da arma para a posição futura do inimigo
		setTurnGunRightRadians(normalRelativeAngle((anguloAbsoluto - getGunHeadingRadians()) + (velocidadeInimigo / velocidadeDisparo))); 
         
        // Gira o radar para seguir o inimigo
        turnRadarRightRadians(Utils.normalRelativeAngle(anguloAbsoluto - getRadarHeadingRadians()));

        // Cria as caracteristicas para mandar pro matodo de previsão de movimento.
		double[] caracteristicas = getCaracteristicas();

        // Envia as caracteristicas para realizar a previsão
        double probabilidade = LR.log(caracteristicas);

		System.out.println("Probabilidade: " + probabilidade);
			
        // Se a minha previsão for maior que 0.5 o robo avança em direção ao inimigo.
		if (probabilidade > 0.5) {	
			moveAtaque(50, distancia);
        } else {
			moveAtaque(-50, distancia);
        }		
        
		fire(potenciaTiro(distancia)); 
    }

    public void onHitByBullet(HitByBulletEvent e) {
        setBack(100);
    }

    public void onHitWall(HitWallEvent e) {
        setBack(20);
        setTurnRight(90);
        setAhead(15);
        setTurnLeft(90);
     }

    // Obtêm as últimas características coletadas
    private double[] getCaracteristicas() {		
        double e_distancia = E_distancia.isEmpty() ? 0 : E_distancia.get(E_distancia.size() - 1);
        double e_angulo = E_angulo.isEmpty() ? 0 : E_angulo.get(E_angulo.size() - 1);
        double e_velocidade = E_velocidade.isEmpty() ? 0 : E_velocidade.get(E_velocidade.size() - 1);
        double r_direcao = R_direcao.isEmpty() ? 0 : R_direcao.get(R_direcao.size() - 1);
        double e_direcao = E_direcao.isEmpty() ? 0 : E_direcao.get(E_direcao.size() - 1);

        return new double[]{e_distancia, e_angulo, e_velocidade, r_direcao, e_direcao};
    }

    // Move em direção ao inimigo
    private void moveAtaque(double movimento, double distanciaInimigo) {
    
        // Calcular a posição futura do robô
        double futureX = getX() + Math.sin(anguloAbsoluto) * distanciaInimigo;
        double futureY = getY() + Math.cos(anguloAbsoluto) * distanciaInimigo;
        
        // Verificar se o robô irá bater na parede
        if (futureX > margem && futureX < mapaLargura - margem &&
            futureY > margem && futureY < mapaAltura - margem) {
            
            // Se a distância ao inimigo for maior que a distância limite ele avança
            if (distanciaInimigo > 150) {
                setTurnRightRadians(Utils.normalRelativeAngle(anguloAbsoluto - getHeadingRadians()));
                setAhead(distanciaInimigo - 150);
            } else {
                // quando chegar na distância limite ele irá circular o inimigo
                setTurnRightRadians(Utils.normalRelativeAngle(anguloAbsoluto - getHeadingRadians() + Math.PI / 2));
                setAhead(movimento);
            }
        } else {
            // Desvia do limite da arena
            if (futureX <= margem || futureX >= mapaLargura - margem) {
                setTurnRight(90);
            } else if (futureY <= margem || futureY >= mapaAltura - margem) {
                setTurnLeft(90);
            }
            setAhead(movimento);
        }
    }


    private void ajustarConfiguracoes() {
        setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setTurnRadarRight(360);
    }


    // Ajusta a potência do tiro de acordo com a distância do inimigo
    private double potenciaTiro(double distancia) {
        if (distancia <= 60) {
            return 3;
        } else if (distancia <= 500) {
            return 2;
        } else {
            return 1;
        }
    }
	
}
