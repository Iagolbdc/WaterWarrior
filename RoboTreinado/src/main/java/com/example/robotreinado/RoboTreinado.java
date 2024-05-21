/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.example.robotreinado;

import robocode.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RoboTreinado extends AdvancedRobot {
private double[] weightsMovimento;
    private double[] weightsAtaque;

    public void run() {
        weightsMovimento = carregarModelo(getDataFile("modelo_movimento.txt"));
        weightsAtaque = carregarModelo(getDataFile("modelo_ataque.txt"));

        while (true) {
            setAhead(100);
            setTurnGunRight(360);
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double[] instanciaMovimento = {1.0, e.getDistance(), e.getVelocity(), e.getHeading(), getEnergy(), getHeading()};
        double probabilidadeMovimento = prever(weightsMovimento, instanciaMovimento);

        if (probabilidadeMovimento > 0.5) {
            setTurnRight(90);
            setAhead(100);
        } else {
            setTurnLeft(90);
            setAhead(100);
        }

        double[] instanciaAtaque = {1.0, e.getDistance(), e.getVelocity(), e.getHeading(), e.getEnergy(), getEnergy(), getGunHeat()};
        double probabilidadeAtaque = prever(weightsAtaque, instanciaAtaque);
        System.out.println(probabilidadeAtaque);
        if (probabilidadeAtaque > 0.5) {
            fire(1);
        }
        execute();
    }

    public static double[] carregarModelo(File file) {
        List<Double> weightsList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                weightsList.add(Double.parseDouble(linha));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double[] weights = new double[weightsList.size()];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = weightsList.get(i);
        }
        return weights;
    }

    public static double prever(double[] weights, double[] instancia) {
        return sigmoid(dotProduct(weights, instancia));
    }

    public static double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }

    public static double dotProduct(double[] a, double[] b) {
        double result = 0.0;
        for (int i = 0; i < a.length; i++) {
            result += a[i] * b[i];
        }
        return result;
    }
}
