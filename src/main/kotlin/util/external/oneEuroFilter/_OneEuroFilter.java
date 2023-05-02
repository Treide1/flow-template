package util.external.oneEuroFilter;

/**
 * OneEuroFilter is a simple filter for noisy signals.
 * <p>
 * Original paper: <a href="https://gery.casiez.net/1euro/">1€ Filter</a>
 * <p>
 * This code is licensed under a BSD license.
 * See the LICENSE file for more information.
 */
public class _OneEuroFilter {

    double freq;
    double mincutoff;
    double beta_;
    double dcutoff;
    _LowPassFilter x;
    _LowPassFilter dx;
    double lasttime;
    static double UndefinedTime = -1;

    double alpha(double cutoff) {
        double te = 1.0 / freq;
        double tau = 1.0 / (2 * Math.PI * cutoff);
        return 1.0 / (1.0 + tau / te);
    }

    void setFrequency(double f) throws Exception {
        if (f <= 0) {
            throw new Exception("freq should be >0");
        }
        freq = f;
    }

    void setMinCutoff(double mc) throws Exception {
        if (mc <= 0) {
            throw new Exception("mincutoff should be >0");
        }
        mincutoff = mc;
    }

    void setBeta(double b) {
        beta_ = b;
    }

    void setDerivateCutoff(double dc) throws Exception {
        if (dc <= 0) {
            throw new Exception("dcutoff should be >0");
        }
        dcutoff = dc;
    }

    public _OneEuroFilter(double freq) throws Exception {
        init(freq, 1.0, 0.0, 1.0);
    }

    public _OneEuroFilter(double freq, double mincutoff) throws Exception {
        init(freq, mincutoff, 0.0, 1.0);
    }

    public _OneEuroFilter(double freq, double mincutoff, double beta_) throws Exception {
        init(freq, mincutoff, beta_, 1.0);
    }

    public _OneEuroFilter(double freq, double mincutoff, double beta_, double dcutoff) throws Exception {
        init(freq, mincutoff, beta_, dcutoff);
    }

    private void init(double freq,
                      double mincutoff, double beta_, double dcutoff) throws Exception {
        setFrequency(freq);
        setMinCutoff(mincutoff);
        setBeta(beta_);
        setDerivateCutoff(dcutoff);
        x = new _LowPassFilter(alpha(mincutoff));
        dx = new _LowPassFilter(alpha(dcutoff));
        lasttime = UndefinedTime;
    }

    double filter(double value) throws Exception {
        return filter(value, UndefinedTime);
    }

    double filter(double value, double timestamp) throws Exception {
        // update the sampling frequency based on timestamps
        if (lasttime != UndefinedTime && timestamp != UndefinedTime) {
            freq = 1.0 / (timestamp - lasttime);
        }

        lasttime = timestamp;
        // estimate the current variation per second
        double dvalue = x.hasLastRawValue() ? (value - x.lastRawValue()) * freq : 0.0; // FIXME: 0.0 or value?
        double edvalue = dx.filterWithAlpha(dvalue, alpha(dcutoff));
        // use it to update the cutoff frequency
        double cutoff = mincutoff + beta_ * Math.abs(edvalue);
        // filter the given value
        return x.filterWithAlpha(value, alpha(cutoff));
    }

    /*
    public static void main(String[] args) throws Exception {
        //randSeed();
        double duration = 10.0; // seconds
        double frequency = 120; // Hz
        double mincutoff = 1.0; // FIXME
        double beta = 1.0;      // FIXME
        double dcutoff = 1.0;   // this one should be ok

        System.out.print(
                "#SRC OneEuroFilter.java" + "\n"
                        + "#CFG {'beta': " + beta + ", 'freq': " + frequency + ", 'dcutoff': " + dcutoff + ", 'mincutoff': " + mincutoff + "}" + "\n"
                        + "#LOG timestamp, signal, noisy, filtered" + "\n");

        _OneEuroFilter f = new _OneEuroFilter(frequency,
                mincutoff,
                beta,
                dcutoff);
        for (double timestamp = 0.0; timestamp < duration; timestamp += 1.0 / frequency) {
            double signal = Math.sin(timestamp);
            double noisy = signal + (Math.random() - 0.5) / 5.0;
            double filtered = f.filter(noisy, timestamp);
            System.out.println("" + timestamp + ", "
                    + signal + ", "
                    + noisy + ", "
                    + filtered);
        }
    }
     */
}