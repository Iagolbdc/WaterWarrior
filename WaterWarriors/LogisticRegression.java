package WaterWarriors;

public class LogisticRegression {

	private double[] weights;
	private double intercept;

	//Construtor para a classe LogisticRegression.

	public LogisticRegression(double[] w, double i) {
		this.intercept = i;
		this.weights = w;
	}

	//Função sigmoide para mapeamento de valor real para [0, 1].

	private static double sigmoid(double x) {
		return 1.0 / (1.0 + Math.exp(-x));
	}

	// Método que de fato realiza a predição.
    // Basicamente ele pega o resultado do valor final das caracteristicas(Já calculada com os seus coeficientes).
    // Soma esse valor ao intercepto para ajustar a fronteira de decisão.
    // Manda para a função de ativação(sigmoid) para transformar o resultado final em um valor entre 0 e 1.
	public double log(double[] x) {
		double logit = .0;
		for (int i=0; i<weights.length;i++)  {
			logit += weights[i] * x[i];
		}
		return sigmoid(logit + intercept);
	}
}
