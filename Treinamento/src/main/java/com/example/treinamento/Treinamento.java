package com.example.treinamento;

//import robocode.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Treinamento {

    private static final int NUM_ITERATIONS = 1000;
    private static final double LEARNING_RATE = 0.01;
    
    public static void main(String[] args) {
        List<double[]> dadosMovimento = lerDadosCSV("dados_movimento.csv");
        List<double[]> dadosAtaque = lerDadosCSV("dados_ataque.csv");
        
        double[] weightsMovimento = treinarModelo(dadosMovimento);
        salvarModelo(weightsMovimento, "modelo_movimento.txt");
        
        double[] weightsAtaque = treinarModelo(dadosAtaque);
        salvarModelo(weightsAtaque, "modelo_ataque.txt");
    }
    
    public static List<double[]> lerDadosCSV(String caminho) {
        List<double[]> dados = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] valores = linha.split(",");
                double[] instancia = new double[valores.length];
                for (int i = 0; i < valores.length; i++) {
                    instancia[i] = Double.parseDouble(valores[i]);
                }
                dados.add(instancia);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dados;
    }
    
    public static double[] treinarModelo(List<double[]> dados) {
        int numFeatures = dados.get(0).length - 1; // Última coluna é o rótulo
        double[] weights = new double[numFeatures + 1]; // +1 para o intercepto

        for (int iter = 0; iter < NUM_ITERATIONS; iter++) {
            double[] gradients = new double[weights.length];

            for (double[] instancia : dados) {
                double[] x = new double[weights.length];
                x[0] = 1.0; // Intercepto
                System.arraycopy(instancia, 0, x, 1, numFeatures);

                double y = instancia[numFeatures];
                double prediction = sigmoid(dotProduct(weights, x));

                for (int j = 0; j < weights.length; j++) {
                    gradients[j] += (prediction - y) * x[j];
                }
            }

            for (int j = 0; j < weights.length; j++) {
                weights[j] -= LEARNING_RATE * gradients[j] / dados.size();
            }
        }

        return weights;
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

    public static void salvarModelo(double[] weights, String caminho) {
        try (FileWriter writer = new FileWriter(caminho)) {
            for (double weight : weights) {
                writer.write(weight + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
