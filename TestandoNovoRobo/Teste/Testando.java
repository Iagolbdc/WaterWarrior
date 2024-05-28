package Teste;

import robocode.*;
import java.awt.Color;
import robocode.util.Utils;

public class Testando extends AdvancedRobot {
    private double direcaoMovimento = 1;
    private double[] coeficientesDeAtaque = {-6.77, -1.71}; // Pesos das caracteristicas para a previsão de ataque. Ou seja é a importancia que cada caracteristica possui na previsão.
    private double interceptoDeAtaque = 2.57; // É o valor que permite ajustar a posição da fronteira de decisão da função de regressão logística
    private long ultimoTempoColisao = 0;
    private static final long TEMPO_MAXIMO_COLISAO = 2000; 
    private static final double DISTANCIA_SEGURA_PAREDE = 100;

    public void run() {
        setColors(Color.BLUE, Color.ORANGE, Color.ORANGE);
        ajustarConfiguracoes();

        while (true) {
            setTurnRadarRight(Double.POSITIVE_INFINITY); 
            mover();
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double anguloAbsoluto = getHeading() + e.getBearing();
        double anguloArma = Utils.normalRelativeAngleDegrees(anguloAbsoluto - getGunHeading());
        double anguloRadar = Utils.normalRelativeAngleDegrees(anguloAbsoluto - getRadarHeading());
        double temperaturaArma = getGunHeat();
        setTurnRadarRight(anguloRadar);
        
        if (!(Math.abs(anguloArma) <= 2)) {
            setTurnGunRight(anguloArma);
        }

        // Cria as caracteristicas para mandar pro matodo de previsão de movimento.
        double[] caracteristicas = {temperaturaArma, anguloArma};

        // Evia as caracteristicas para realizar a previsão se o robo vai ou não atirar.
        double previsao = preverMovimento(caracteristicas);
        // Se a minha previsão de ataque for maior ou igual a 0.5 o robo ataca.
        if (previsao >= 0.5) {
            setTurnGunRight(anguloArma);
            fire(Math.min(400 / e.getDistance(), 3));
        }

        if (e.getDistance() > 150) {
            ajustarDirecaoMovimento(e.getBearing());
        } else {
            setBack(50);
            evitarParedeAoAproximarInimigo();
        }

        setTurnRight(anguloAbsoluto + 90 - (15 * direcaoMovimento));
        execute();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        mudarDirecaoMovimento();
    }

    public void onHitWall(HitWallEvent e) {
        long tempoAtual = getTime();
        if (tempoAtual - ultimoTempoColisao > TEMPO_MAXIMO_COLISAO) {
            mudarDirecaoMovimento();
            setBack(200 * direcaoMovimento);
            ultimoTempoColisao = tempoAtual;
        }
    }

    public void onHitRobot(HitRobotEvent e) {
        if (e.isMyFault()) {
            setBack(50);
        }
    }

    private void ajustarConfiguracoes() {
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
    }

    private void mover() {
        if (estaProximoParede()) {
            evitarParede();
        } else {
            setAhead(100 * direcaoMovimento);
        }
    }

    private boolean estaProximoParede() {
        double x = getX();
        double y = getY();
        double larguraCampo = getBattleFieldWidth();
        double alturaCampo = getBattleFieldHeight();

        return x <= DISTANCIA_SEGURA_PAREDE || x >= larguraCampo - DISTANCIA_SEGURA_PAREDE ||
                y <= DISTANCIA_SEGURA_PAREDE || y >= alturaCampo - DISTANCIA_SEGURA_PAREDE;
    }

    private void evitarParede() {
        double x = getX();
        double y = getY();
        double larguraCampo = getBattleFieldWidth();
        double alturaCampo = getBattleFieldHeight();
        double anguloAtual = getHeading();

        if (estaNoCanto(x, y, larguraCampo, alturaCampo)) {
            setTurnRight(45); 
            setAhead(100);
        } else {
            double anguloParaCentro = Math.toDegrees(Math.atan2(alturaCampo / 2 - y, larguraCampo / 2 - x));
            setTurnRight(Utils.normalRelativeAngleDegrees(anguloParaCentro - anguloAtual));
            setAhead(100);
        }
    }

    private boolean estaNoCanto(double x, double y, double larguraCampo, double alturaCampo) {
        return (x <= DISTANCIA_SEGURA_PAREDE && y <= DISTANCIA_SEGURA_PAREDE) || 
               (x <= DISTANCIA_SEGURA_PAREDE && y >= alturaCampo - DISTANCIA_SEGURA_PAREDE) ||
               (x >= larguraCampo - DISTANCIA_SEGURA_PAREDE && y <= DISTANCIA_SEGURA_PAREDE) ||
               (x >= larguraCampo - DISTANCIA_SEGURA_PAREDE && y >= alturaCampo - DISTANCIA_SEGURA_PAREDE);
    }

    private void ajustarDirecaoMovimento(double bearing) {
        if (!estaProximoParede()) {
            direcaoMovimento = bearing > -90 && bearing <= 90 ? 1 : -1;
            setAhead(100 * direcaoMovimento);
        } else {
            setBack(50);
        }
    }

    private void evitarParedeAoAproximarInimigo() {
        if (estaProximoParede()) {
            setTurnRight(45);
        }
    }

    private void mudarDirecaoMovimento() {
        direcaoMovimento = -direcaoMovimento;
        setAhead(100 * direcaoMovimento);
    }
    
    // Metodo para combinar as caracteristicas com seu determinado coeficient(peso) gerando assim relevância entre as características.
    private double produtoEscalar(double[] a, double[] b) { 
        double resultado = 0.0;
        for (int i = 0; i < a.length; i++) {
            resultado += a[i] * b[i];
        }
        return resultado;
    }

    // Função de ativação, aonde sera passada a junção das caracteristicas mais o intercepto, para transformar o resultado final em uma probabilidade entre 0 e 1. 
    private double sigmoid(double z) { 
        return 1.0 / (1.0 + Math.exp(-z));
    }

    // Método que de fato realiza a predição.
    // Basicamente ele pega o resultado do valor final das caracteristicas(Já calculada com os seus coeficientes).
    // Soma esse valor ao intercepto para ajustar a fronteira de decisão.
    // Manda para a função de ativação(sigmoid) para transformar o resultado final em um valor entre 0 e 1.
    private double preverMovimento(double[] caracteristicas) { 
        double z = produtoEscalar(caracteristicas, coeficientesDeAtaque) + interceptoDeAtaque;
        return sigmoid(z);
    }
}
