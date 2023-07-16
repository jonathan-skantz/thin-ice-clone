import javax.swing.Timer;

public class TimedCounter {
    
    private Timer timer = new Timer(0, null);

    private float duration;
    public int fps;
    
    public int frame;
    public int frames;

    private Runnable callback;
    
    // TODO: onFinish function

    public TimedCounter(int fps) {
        setup(0, fps);
    }
    
    public TimedCounter(float duration, int fps) { 
        setup(duration, fps);
    }
    
    // duration: in ms
    private void setup(float duration, int fps) {
        this.duration = duration;
        this.fps = fps;
        frames = (int)(fps * duration);
        timer.setDelay((int)(1000/fps));
        timer.addActionListener(e -> { tick(); });
    }

    public void setDuration(float duration) {
        // NOTE: if duration < 0, it will never stop
        this.duration = duration;

        frames = (int)(fps * duration);

        if (frame >= frames) {
            frame = frames;
            timer.stop();
        }
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

    public void tick() {
        frame++;
        callback.run();

        if (frame == frames) {
            timer.stop();
        }
    }

    public void setFPS(int fps) {
        this.fps = fps;

        float progress = (float)frame / frames;
        
        frames = (int)(fps * duration);
        frame = (int)(progress * frames);
        timer.setDelay((int)(1000/fps));
    }

    public void start() {
        // NOTE: auto-stops
        frame = 0;
        timer.start();
    }

    public void reset() {
        timer.stop();
        frame = 0;
    }

}
