/**
 * LowPassFilter is a simple filter for noisy signals.
 * <p>
 * Original paper: <a href="https://gery.casiez.net/1euro/">1€ Filter</a>
 * <p>
 * This code is licensed under a BSD license.
 * See the LICENSE file for more information.
 */
class _LowPassFilter {

    double y, a, s;
    boolean initialized;

    void setAlpha(double alpha) throws Exception {
        if (alpha <= 0.0 || alpha > 1.0) {
            throw new Exception("alpha should be in (0.0., 1.0]");
        }
        a = alpha;
    }

    public _LowPassFilter(double alpha) throws Exception {
        init(alpha, 0);
    }

    public _LowPassFilter(double alpha, double initval) throws Exception {
        init(alpha, initval);
    }

    private void init(double alpha, double initval) throws Exception {
        y = s = initval;
        setAlpha(alpha);
        initialized = false;
    }

    public double filter(double value) {
        double result;
        if (initialized) {
            result = a * value + (1.0 - a) * s;
        } else {
            result = value;
            initialized = true;
        }
        y = value;
        s = result;
        return result;
    }

    public double filterWithAlpha(double value, double alpha) throws Exception {
        setAlpha(alpha);
        return filter(value);
    }

    public boolean hasLastRawValue() {
        return initialized;
    }

    public double lastRawValue() {
        return y;
    }
};

