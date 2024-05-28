package Teste;

import robocode.*;
import java.awt.Color;
import robocode.util.Utils;

public class Testando extends AdvancedRobot {
    private double direcaoMovimento = 1;
    private double[] coeficientes = {-6.77, -1.71};
    private double intercepto = 2.57;
    private long ultimoTempoColisao = 0;
    private static final long TEMPO_MAXIMO_COLISAO = 2000; // Tempo limite entre colisões
    private static final double DISTANCIA_SEGURA_PAREDE = 100;

    public void run() {
        setColors(Color.RED, Color.BLACK, Color.YELLOW); // Corpo, Arma, Radar
        ajustarConfiguracoes();

        while (true) {
            setTurnRadarRight(Double.POSITIVE_INFINITY); // Mantém o radar girando
            mover();
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double anguloAbsoluto = getHeading() + e.getBearing();
        double anguloArma = Utils.normalRelativeAngleDegrees(anguloAbsoluto - getGunHeading());
        double anguloRadar = Utils.normalRelativeAngleDegrees(anguloAbsoluto - getRadarHeading());
        setTurnRadarRight(anguloRadar);

        double temperaturaArma = getGunHeat();
        double anguloDaArma = anguloArma;

        double[] caracteristicas = {temperaturaArma, anguloDaArma};

        double previsao = preverMovimento(caracteristicas);
        if (previsao <= 0.5 && temperaturaArma == 0) {
            setTurnGunRight(anguloArma);
            fire(Math.min(400 / e.getDistance(), 3));
        }

        if (!(Math.abs(anguloArma) <= 2)) {
            setTurnGunRight(anguloArma);
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
            setAhead(100 * direcaoMovimento); // Move para frente normalmente
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
            setTurnRight(45); // Vira para tentar sair do canto
            setAhead(100);
        } else {
            // Ajusta a direção gradualmente
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
            setTurnRight(45); // Vira para tentar sair do canto
        }
    }

    private void mudarDirecaoMovimento() {
        direcaoMovimento = -direcaoMovimento;
        setAhead(100 * direcaoMovimento);
    }

    private double sigmoid(double z) { // Função Sigmoide
        return 1.0 / (1.0 + Math.exp(-z));
    }

    private double preverMovimento(double[] caracteristicas) { // Previsão de movimento
        double z = produtoEscalar(caracteristicas, coeficientes) + intercepto;
        return sigmoid(z);
    }

    private double produtoEscalar(double[] a, double[] b) { // Produto Escalar
        double resultado = 0.0;
        for (int i = 0; i < a.length; i++) {
            resultado += a[i] * b[i];
        }
        return resultado;
    }
}
