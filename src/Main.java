public class Main {

    public static void main(String[] args) {

        Window window = new Window();
        
        // Allow sprites to reference the window, without having to
        // save the reference to every instance of Sprite.
        Sprite.window = window;         
        
        Sprite player = new Sprite("player.png", 10);
        
        Sprite enemy = new Sprite("noimage", 20);
        enemy.moveTo(120, 100);

        // setup key callbacks
        KeyListen listener = new KeyListen();
        window.addKeyListener(listener);

        listener.addCallback("up", () -> {player.move("up");});
        listener.addCallback("down", () -> {player.move("down");});
        listener.addCallback("left", () -> {player.move("left");});
        listener.addCallback("right", () -> {player.move("right");});

        listener.addCallback("test", () -> {System.out.println(player.collidesWith(enemy));});
    }

}
