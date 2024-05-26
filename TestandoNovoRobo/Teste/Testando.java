package Teste;
import robocode.*;
import java.awt.Color;
import robocode.util.Utils;
import java.io.*;

public class Testando extends AdvancedRobot {
    private double moveDirection = 1;
    private PrintWriter csvAtaque;

    public void run() {
        setColors(Color.RED, Color.BLACK, Color.YELLOW); // Body, Gun, Radar
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        try{
			RobocodeFileOutputStream rfosAt = new RobocodeFileOutputStream(getDataFile("dados_ataque.csv"));
			csvAtaque = new PrintWriter (new BufferedWriter(new OutputStreamWriter(rfosAt))); 
			
			csvAtaque.println("temperaturaArma,anguloArma,atirou");
		} catch (IOException e){
			e.printStackTrace();
		}

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
        int atirou = 0;
        setTurnRadarRight(bearingFromRadar);

        if (Math.abs(bearingFromGun) <= 2) {
            setTurnGunRight(bearingFromGun);
            if (getGunHeat() == 0) {
                fire(Math.min(400 / e.getDistance(), 3));
                atirou = 1;
            }
        } else {
            setTurnGunRight(bearingFromGun);
        }

        if (e.getDistance() > 150) {
            moveDirection = e.getBearing() > -90 && e.getBearing() <= 90 ? 1 : -1;
            setAhead(100 * moveDirection);
        } else {
            setBack(50);
        }

        setTurnRight(e.getBearing() + 90 - (15 * moveDirection));
        salvarDados(arredondaValor(getGunHeat()), arredondaValor(Math.abs(bearingFromGun)), atirou);
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
        double margin = 60; // Distance from the wall

        // Check if the robot is too close to the walls and adjust
        if (x <= margin) {
            setTurnRight(Utils.normalRelativeAngleDegrees(90 - getHeading())); // Turn right
            setAhead(100);
        } else if (x >= battlefieldWidth - margin) {
            setTurnRight(Utils.normalRelativeAngleDegrees(270 - getHeading())); // Turn left
            setAhead(100);
        } else if (y <= margin) {
            setTurnRight(Utils.normalRelativeAngleDegrees(180 - getHeading())); // Turn around
            setAhead(100);
        } else if (y >= battlefieldHeight - margin) {
            setTurnRight(Utils.normalRelativeAngleDegrees(0 - getHeading())); // Continue moving
            setAhead(100);
        } else {
            setAhead(100 * moveDirection); // Move forward normally
        }
    }

    private double arredondaValor(double valor) {
        return Math.round(Math.abs(valor) * 100.0) / 100.0;
    }

    private void salvarDados(double temperaturaArma, double anguloArma, int atirou) {
        if(csvAtaque != null){
			csvAtaque.println(temperaturaArma + "," + anguloArma + "," + atirou);
          	csvAtaque.flush();
		}
    }

    public void onDeath(DeathEvent event) {
		if (csvAtaque != null) {
            csvAtaque.close();
            csvAtaque = null; // Libera o recurso
        }
    }

    public void onWin(WinEvent event) {
        if (csvAtaque != null) {
            csvAtaque.close();
            csvAtaque = null; // Libera o recurso
        }
    }
}
