package robo;
import robocode.*;
import static robocode.util.Utils.normalRelativeAngleDegrees;
import static robocode.util.Utils.normalRelativeAngle;
import robocode.util.Utils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;  
import java.io.*;

public class MyRobo extends AdvancedRobot { 					 // Coeficientes e intercepto
    private static final double[] coeficientes = {0.01, -0.01, 0.02, 0.01, -0.02}; 
    private static final double intercepto = 0.5;
    private static final double margemParede = 60;

    private List<Double> distancias = new ArrayList<Double>();
    private List<Double> angulos = new ArrayList<Double>();
    private List<Double> velocidades = new ArrayList<Double>();
    private List<Double> roboDireçoes = new ArrayList<Double>();
    private List<Double> inimigoDireçoes = new ArrayList<Double>();
	
	private PrintWriter writer;
	
	private double bulletPower;
	public boolean hitBullet;
		
    public void run() {

		try{
			RobocodeFileOutputStream csvDados = new RobocodeFileOutputStream(getDataFile("csvDados.csv"));
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(csvDados)));
			writer.println("distancia,angulo,velocidade,direcao_robo,direcao_inimigo,fui_acertado\n");
		}catch(IOException e){
			e.printStackTrace();
		}
		
		setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		
		setAhead(40000);
		setTurnRadarRight(360);

        while (true) {

            if (getRadarTurnRemaining() == 0.0) {
				setTurnRadarRight(360);
			} 
			
			execute();
        }
    }
	
	public void moveAndRecord(PrintWriter writer, double[] features, double distance){
		moveSafely(distance);
		System.out.println("Aqui");
		int safe = hitBullet ? 0 : 1;
		writer.println(features[0] + "," + features[1] + "," + features[2] + "," + features[3] + "," + features[4] + "," + safe);
		hitBullet = false;
	}

    public void onScannedRobot(ScannedRobotEvent e) {   		// Regressão Logística na Decisão de Movimento
        double distancia = e.getDistance();
        double angulo = e.getBearing();
        double velocidade = e.getVelocity();
        double inimigoDireçao = e.getHeading();
        double energia = e.getEnergy();
		
		
		
		double bulletVelocity = 20 - 3 * firePower(distancia);
		
		double enemyPositionInRadians = getHeadingRadians() + e.getBearingRadians();
		
		double enemyLateralVelocity = velocidade * Math.sin(e.getHeadingRadians() - enemyPositionInRadians); 
		
		setTurnGunRightRadians(normalRelativeAngle((enemyPositionInRadians - getGunHeadingRadians()) + (enemyLateralVelocity / bulletVelocity))); 

		double radarTurn = Utils.normalRelativeAngle(enemyPositionInRadians - getRadarHeadingRadians());

		double extraTurn = Math.min(Math.atan(36.0 / e.getDistance()), Rules.RADAR_TURN_RATE_RADIANS);

		radarTurn += (radarTurn < 0 ? -extraTurn : extraTurn);

		setTurnRadarRightRadians(radarTurn);
		

		double[] features = getFeatures();
        double probabilidade = predictMovement(features);
		System.out.println("Probabilidade: " + probabilidade);
			
		if (probabilidade > 0.5) {
                //moveSafely(50);		
		moveAndRecord(writer, features, 50);
        } else {
			moveAndRecord(writer, features, -50);
                //moveSafely(-50);
        }		
			
        distancias.add(distancia);
        angulos.add(angulo);
        velocidades.add(velocidade);
        inimigoDireçoes.add(inimigoDireçao);

        if (distancia < 100) {
            double gunTurn = getHeading() + angulo - getGunHeading();
        } else {
            double tempo = distancia / (20 - 3 * firePower(distancia));
            double futuraPosicaoX = getX() + distancia * Math.sin(Math.toRadians(getHeading() + angulo)) + Math.sin(Math.toRadians(inimigoDireçao)) * velocidade * tempo;
            double futuraPosicaoY = getY() + distancia * Math.cos(Math.toRadians(getHeading() + angulo)) + Math.cos(Math.toRadians(inimigoDireçao)) * velocidade * tempo;
            double anguloParaAtirar = Math.toDegrees(Math.atan2(futuraPosicaoX - getX(), futuraPosicaoY - getY()));
        }
        
		fire(firePower(distancia));
    }

    public void onHitByBullet(HitByBulletEvent e) {
		hitBullet = true;
        setBack(50);
        setTurnRight(30);
    }

    public void onHitWall(HitWallEvent e) {
        setBack(20);
        setTurnRight(90);
        setAhead(15);
        setTurnRight(90);
     }

    private double sigmoid(double z) { 			// Função Sigmoide
        return 1.0 / (1.0 + Math.exp(-z));
    }

    private double dotProduct(double[] a, double[] b) {		// Produto Escalar
        double resultado = 0.0;
        for (int i = 0; i < a.length; i++) {
            resultado += a[i] * b[i];
        }
        return resultado;
    }

    private double predictMovement(double[] features) {		// Previsão de movimento
        double z = dotProduct(features, coeficientes) + intercepto;
        return sigmoid(z);
    }

    private double[] getFeatures() {		// Extraindo as características do robô inimigo
        double ultimaDistancia = distancias.isEmpty() ? 0 : distancias.get(distancias.size() - 1);
        double ultimoAngulo = angulos.isEmpty() ? 0 : angulos.get(angulos.size() - 1);
        double ultimaVelocidade = velocidades.isEmpty() ? 0 : velocidades.get(velocidades.size() - 1);
        double ultimaDireçaoRobo = roboDireçoes.isEmpty() ? 0 : roboDireçoes.get(roboDireçoes.size() - 1);
        double ultimaDireçaoInimigo = inimigoDireçoes.isEmpty() ? 0 : inimigoDireçoes.get(inimigoDireçoes.size() - 1);

        return new double[]{ultimaDistancia, ultimoAngulo, ultimaVelocidade, ultimaDireçaoRobo, ultimaDireçaoInimigo};
    }

    private void moveSafely(double distancia) {
        double futureX = getX() + Math.sin(Math.toRadians(getHeading())) * distancia;
        double futureY = getY() + Math.cos(Math.toRadians(getHeading())) * distancia;
        if (futureX > margemParede && futureX < getBattleFieldWidth() - margemParede &&
            futureY > margemParede && futureY < getBattleFieldHeight() - margemParede) {
            setAhead(distancia);
        } else {
            if (futureX <= margemParede || futureX >= getBattleFieldWidth() - margemParede) {
            setTurnRight(90);
        } else if (futureY <= margemParede || futureY >= getBattleFieldHeight() - margemParede) {
            setTurnLeft(90);
        }
            setAhead(50);
        }
    }

    private double firePower(double distancia) {
        if (distancia <= 60) {
            return 3;
        } else if (distancia <= 500) {
            return 2;
        } else {
            return 1;
        }
    }
	
		public void onDeath(DeathEvent event) {
			// Fecha o writer quando o robô morre
			if (writer != null) {
				writer.close();
				writer = null; // Libera o recurso
			}
		}
		
		public void onWin(WinEvent event) {
			// Fecha o writer quando o robô ganha
			if (writer != null) {
				writer.close();
				writer = null; // Libera o recurso
			}
		}
}