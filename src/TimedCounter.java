import javax.swing.Timer;

public class TimedCounter {
    
    private Timer timer = new Timer(0, null);

    private float duration;
    public int fps;
    
    public int frame;
    public int frames;
    public float onFinishDelay;

    public boolean finished;

    public void onStart() {}
    public void onTick() {}
    public void onFinish() {}

    public TimedCounter(int fps) {
        setup(0, fps);
    }
    
    public TimedCounter(float duration, int fps) { 
        setup(duration, fps);
    }
    
    // duration: in seconds
    private void setup(float duration, int fps) {
        this.duration = duration;
        this.fps = fps;

        updateAmountFrames();

        timer.setDelay((int)(1000/fps));
        timer.addActionListener(e -> { tick(); });
    }

    private void updateAmountFrames() {
        if (duration < 0) {
            frames = -1;
        }
        else {
            setFrames((int)(fps * duration));
        }
    }
    
    private void setFrames(int frames) {
        this.frames = Math.max(1, frames);   // asserts at least one frame (if duration is short)
    
        if (frame >= frames) {
            frame = frames;
            timer.stop();
        }

    }

    public void setFramesAndPreserveDuration(int frames) {
        setFrames(frames);
        fps = (int)Math.ceil(frames / duration);    // rounds up to assure all frames will be ticked
    }

    public void setFramesAndPreserveFPS(int frames) {
        setFrames(frames);
        duration = frames / fps;
    }

    public void setDuration(float duration) {
        this.duration = duration;
        updateAmountFrames();
    }

    public double oneFrameInSeconds() {
        return duration / frames;
    }

    public void tick() {

        frame++;
        onTick();

        if (frame == frames) {
            timer.stop();
            finished = true;
            
            if (onFinishDelay > 0) {
                Timer delayTimer = new Timer((int)(onFinishDelay * 1000), null);
                delayTimer.addActionListener(e -> {
                    onFinish();
                    delayTimer.stop();
                });

                delayTimer.start();
            }
            else {
                onFinish();
            }
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
        finished = false;
        onStart();
        timer.start();
    }
    
    public void reset() {
        timer.stop();
        frame = 0;
        finished = false;
    }

}
