package Treinamento;
import robocode.*;

import java.io.*;
 
public class RoboDados extends AdvancedRobot
{

	private PrintWriter csvMovimento;
	private PrintWriter csvAtaque;

    public void run() {
	
		try{
			RobocodeFileOutputStream rfosMv = new RobocodeFileOutputStream(getDataFile("dados_movimento.csv"));           
			RobocodeFileOutputStream rfosAt = new RobocodeFileOutputStream(getDataFile("dados_ataque.csv"));
			csvMovimento = new PrintWriter(new BufferedWriter(new OutputStreamWriter(rfosMv))); 
			csvAtaque = new PrintWriter (new BufferedWriter(new OutputStreamWriter(rfosAt))); 
			
			csvMovimento.println("time,distancia,velocidade,direcao,minhaEnergia,meuHeading");
			csvAtaque.println("time,distancia,velocidade,direcao,energia,minhaEnergia,meuGunHeat");
		} catch (IOException e){
			e.printStackTrace();
		}
        while (true) {
            setAhead(100);
            setTurnGunRight(360);
            waitFor(new MoveCompleteCondition(this));
            setBack(100);
            setTurnGunRight(360);
            waitFor(new MoveCompleteCondition(this));
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double distancia = e.getDistance();
        double velocidade = e.getVelocity();
        double direcao = e.getHeading();
        double energia = e.getEnergy();
        double minhaEnergia = getEnergy();
        double meuHeading = getHeading();
        double meuGunHeat = getGunHeat();

        // Salvando dados de movimento
            
		if(csvMovimento != null){
			csvMovimento.println(getTime() + "," + distancia + "," + velocidade + "," + direcao + "," + minhaEnergia + "," + meuHeading);
           	csvMovimento.flush();
		}
			
			// Salvando dados de ataque
		if(csvAtaque != null){
			csvAtaque.println(getTime() + "," + distancia + "," + velocidade + "," + direcao + "," + energia + "," + minhaEnergia + "," + meuGunHeat);
          	csvAtaque.flush();
		}
    }
	
	public void onDeath(DeathEvent event) {
        // Fecha o writer quando o robô morre
        if (csvMovimento != null) {
            csvMovimento.close();
            csvMovimento = null; // Libera o recurso
        }
		
		if (csvAtaque != null) {
            csvAtaque.close();
            csvAtaque = null; // Libera o recurso
        }
    }

    public void onWin(WinEvent event) {
        // Fecha o writer quando o robô ganha
        if (csvMovimento != null) {
            csvMovimento.close();
            csvMovimento = null; // Libera o recurso
        }
		
        if (csvAtaque != null) {
            csvAtaque.close();
            csvAtaque = null; // Libera o recurso
        }
    }
}
