package Teste;
import robocode.*;
import java.awt.Color;
import robocode.util.Utils;
import java.io.*;

public class Testando extends AdvancedRobot {
    private double moveDirection = 1;
    private double[] coeficientes = {-6.77, -1.71}; 
    private double intercepto = 2.57; 

    public void run() {
        setColors(Color.RED, Color.BLACK, Color.YELLOW); // Body, Gun, Radar
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            setTurnRadarRight(Double.POSITIVE_INFINITY); // Keep turning radar
            move();
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double absoluteBearing = getHeading() + e.getBearing();
        double bearingFromGun = Utils.normalRelativeAngleDegrees(absoluteBearing - getGunHeading());
        double bearingFromRadar = Utils.normalRelativeAngleDegrees(absoluteBearing - getRadarHeading());
        setTurnRadarRight(bearingFromRadar);

        double temperaturaArma = getGunHeat();
        double anguloDaArma = bearingFromGun;

        double[] features = {temperaturaArma, anguloDaArma};

        double predict = predictMovement(features);
        if (predict <= 0.5) {
            setTurnGunRight(bearingFromGun);
            if (temperaturaArma == 0) {
                fire(Math.min(400 / e.getDistance(), 3));
            }
        }

        if(!(Math.abs(bearingFromGun) <= 2)) {
            setTurnGunRight(bearingFromGun);
        }

        if (e.getDistance() > 150) {
            moveDirection = e.getBearing() > -90 && e.getBearing() <= 90 ? 1 : -1;
            setAhead(100 * moveDirection);
        } else {
            setBack(50);
        }

        setTurnRight(e.getBearing() + 90 - (15 * moveDirection));
        execute();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        moveDirection = -moveDirection;
        setAhead(100 * moveDirection);
    }

    public void onHitWall(HitWallEvent e) {
        moveDirection = -moveDirection;
        setBack(200 * moveDirection);
    }

    public void onHitRobot(HitRobotEvent e) {
        if (e.isMyFault()) {
            setBack(50);
        }
    }

    private void move() {
    double x = getX();
    double y = getY();
    double battlefieldWidth = getBattleFieldWidth();
    double battlefieldHeight = getBattleFieldHeight();
    double margin = 60; // Distância da parede
    double heading = getHeading();

    // Verifica se está próximo da parede em tempo real e ajusta a direção
    if (x <= margin || x >= battlefieldWidth - margin ||
            y <= margin || y >= battlefieldHeight - margin) {
        // Evita que fique preso nos cantos
        if ((x <= margin && y <= margin) || (x <= margin && y >= battlefieldHeight - margin) ||
                (x >= battlefieldWidth - margin && y <= margin) || (x >= battlefieldWidth - margin && y >= battlefieldHeight - margin)) {
            setTurnRight(45); // Vira para tentar sair do canto
            setAhead(100);
        } else {
            // Ajusta a direção gradualmente
            double angleToCenter = Math.toDegrees(Math.atan2(battlefieldHeight / 2 - y, battlefieldWidth / 2 - x));
            setTurnRight(Utils.normalRelativeAngleDegrees(angleToCenter - heading));
            setAhead(100);
        }
    } else {
        setAhead(100 * moveDirection); // Move para frente normalmente
    }
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
}
