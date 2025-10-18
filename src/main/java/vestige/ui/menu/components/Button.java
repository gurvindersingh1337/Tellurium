package vestige.ui.menu.components;

import lombok.Getter;
import vestige.util.misc.TimerUtil;

@Getter
public class Button {

    private String name;
    private boolean hovered;

    private final TimerUtil animationTimer = new TimerUtil();
    private final long animInDuration = 200;
    private final long animOutDuration = 70;

    private boolean animationDone;
    private double mult;

    public Button(String name) {
        this.name = name;
        this.hovered = false;
        this.animationDone = true;
        this.mult = 0;
    }

    public void updateState(boolean state) {
        if (hovered != state) {
            hovered = state;
            animationTimer.reset();
            animationDone = false;
        }

        if (animationTimer.getTimeElapsed() >= (hovered ? animInDuration : animOutDuration)) {
            animationDone = true;
        }

        mult = getMult();
    }

    public double getMult() {
        long elapsed = animationTimer.getTimeElapsed();
        double progress;

        if (hovered) {
            progress = Math.min((double) elapsed / animInDuration, 1.0);
        } else {
            progress = Math.min((double) (animOutDuration - elapsed) / animOutDuration, 1.0);
        }

        return Math.max(0, progress);
    }

    public void setMult(double mult) {
        this.mult = Math.max(0, Math.min(mult, 1));
    }

    public void setAnimationDone(boolean done) {
        this.animationDone = done;
    }

    public boolean isAnimationDone() {
        return animationDone;
    }

    public boolean isHovered() {
        return hovered;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
