import javax.swing.Timer;

public class TimedCounter {
    
    private Timer timer = new Timer(0, null);

    private float duration = 0;
    public int fps;
    
    public int frame = 0;
    public int frames;

    public TimedCounter() {}

    public TimedCounter(int fps) {
        this.fps = fps;
        timer.setDelay((int)1000/fps);
    }

    // duration: in ms
    public TimedCounter(float duration, int fps) { 
        this.duration = duration;
        this.fps = fps;
        frames = (int)(fps * duration);
        timer.setDelay((int)(1000/fps));
    }

    public void setDuration(float duration) {
        this.duration = duration;

        frames = (int)(fps * duration);

        if (frame >= frames) {
            frame = frames;
            timer.stop();
        }
    }

    public void setCallback(Runnable callback) {

        if (timer.getActionListeners().length > 0) {
            timer.removeActionListener(timer.getActionListeners()[0]);
        }

        timer.addActionListener(e -> {

            frame++;
            callback.run();

            if (frame == frames) {
                timer.stop();
            }
            
        });
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

}
