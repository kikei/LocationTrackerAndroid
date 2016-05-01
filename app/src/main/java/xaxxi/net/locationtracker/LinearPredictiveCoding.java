package net.xaxxi.locationtracker;

import java.util.Arrays;

public class LinearPredictiveCoding {
    int mLPC = 0;
    double[] mData = null;
    double[] mR = null;
    double[] mA = null;
    double[] mE = null;

    public LinearPredictiveCoding(int N, double[] data) {
        mData = data;
        mLPC = N;
    }
    
    public double[] getAutoCorrelation() {
        if (mR == null)
            this.autoCorrelation();
        return mR;
    }

    public double[] getLPCCoefficients() {
        if (mA == null)
            this.livinsonDurbin();
        return mA;
    }

    public double[] getLPCError() {
        if (mE == null)
            this.livinsonDurbin();
        return mE;
    }

    public void autoCorrelation() {
        double[] x = mData;
        double[] r = zeros(x.length);
        for (int l = 0; l < x.length; l++) {
            for (int n = 0; n < x.length-l; n++) {
                r[l] += x[n] * x[n+l];
            }
        }
        mR = r;
    }
    
    public void livinsonDurbin() {
        double[] r = this.getAutoCorrelation();
        int lpc = mLPC;
        
        double[] a = zeros(lpc + 1);
        double[] e = zeros(lpc + 1);

        // Case k = 1
        a[0] = e[0] = 1.0;
        a[1] = -r[1] / r[0];
        e[1] = r[0] + r[1] * a[1];

        // Case 1 < k <= lpc
        for (int k = 1; k < lpc; k++) {
            double l = 0.0;
            for (int j = 0; j < k+1; j++)
                l -= a[j] * r[k+1-j];
            l /= e[k];

            double[] U = zeros(k+2);
            U[0] = 1.0;
            for (int j = 1; j < k+1; j++)
                U[j] = a[j];

            // A = U + λV; V = reverse(U)
            for (int j = 0; j < k+2; j++)
                a[j] = U[j] + l * U[k+1-j];

            e[k+1] = e[k] * (1.0 - l * l);
        }
        mA = a;
        mE = e;
    }

    public double predictNext(double[] y, int n) {
        double[] a = this.getLPCCoefficients();

        android.util.Log.d("LocationTracker.LinearPredictiveCoding",
                           "predictNext" + 
                           " y=[" + Utilities.Debug.joinArray(",", y) + "]" +
                           ", a=" + Utilities.Debug.joinArray(",", a) + "]");

        int k = a.length - 1;
        if (n < k) return 0.0;

        // r(n) = -Σ{a(i)*y(n-i)}
        double t = 0.0;
        for (int i = 1; i < k+1; i++)
            t += a[i] * y[n-i];
        return -t;
    }

    public double predictNext(double[] y) {
        return predictNext(y, y.length);
    }

    public double[] predictAll(double[] y) {
        double[] a = this.getLPCCoefficients();
        int k = a.length - 1;
        double[] r = new double[y.length];
        for (int n = 0; n < k; n++)
            r[n] = 0.0;
        // r(n) = -Σ{a(i)*y(n-i)}
        for (int n = k; n < y.length; n++) {
            double t = 0.0;
            for (int i = 1; i < k+1; i++)
                t += a[i] * y[n-i];
            r[n] = -t;
        }
        return r;
    }

    private static double[] zeros(int n) {
        double[] a = new double[n];
        Arrays.fill(a, 0.0);
        return a;
    }
}
