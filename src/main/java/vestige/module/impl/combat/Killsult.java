package vestige.module.impl.combat;

import vestige.module.Module;
import vestige.module.Category;
import vestige.setting.impl.ModeSetting;
import vestige.event.Listener;
import vestige.event.impl.PacketReceiveEvent;
import net.minecraft.network.play.server.S02PacketChat;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Killsult extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Tellurium", "Tellurium");

    private final List<String> telluriumLines = Arrays.asList(
            "Opal v2 on top",
            "Tellurium clears your trash client",
            "I think your liquid is not bouncing",
            "Yk, get rekt",
            "Get off Lunar Client bud",
            "Your anticheat cant even detect me lmao",
            "Tellurium > your entire inventory",
            "Maybe try not being trash?",
            "Uninstall and go play bedwars",
            "Your combo? Never heard of it",
            "L + ratio + tellurium user",
            "Try updating your client from 2019",
            "My grandma clicks faster than you",
            "Bro got folded by tellurium",
            "Your ping or your skill? Both trash",
            "Tellurium was too much for you",
            "Go back to single player",
            "Did you even try?",
            "Your scaffold cant save you now",
            "Tellurium diff gg",
            "Sit down kid",
            "Your client is mid at best",
            "Maybe enable your hacks next time",
            "Too easy lmfao",
            "Bro thought he had a chance",
            "You got destroyed by a superior client",
            "Your killaura needs an update",
            "Tellurium gaming on top",
            "Stop crying and get better",
            "You just got dominated",
            "Your velocity check failed",
            "Imagine losing to tellurium",
            "Bro is running internet explorer",
            "Your fps or your skill?",
            "Delete system32 might help",
            "You need more than luck buddy",
            "Tellurium simply built different",
            "Your reach is shorter than your patience",
            "Bot detected",
            "Free kill thanks for coming",
            "You thought you could win?",
            "My client > your life",
            "Go touch grass maybe?",
            "Skill issue detected",
            "Tellurium stays undefeated",
            "Your monitor off or something?",
            "Bro fighting ghosts out here",
            "You got packed up",
            "Tellurium superiority confirmed",
            "Your gaming chair broken?",
            "Your existence is a waste of server resources",
            "Bro you're embarrassing your whole bloodline",
            "I've seen NPCs with better decision making",
            "You fight like a disconnected keyboard",
            "Your parents router is faster than your brain",
            "Congratulations you just got violated",
            "You're the reason servers have kick commands",
            "I'd call you trash but that's disrespectful to garbage",
            "Your skill level is in the negatives",
            "Bro got absolutely humiliated",
            "You play like your monitor is a microwave",
            "I'm convinced you're playing with your feet",
            "Your combo is as real as your chances of winning",
            "This is just sad at this point",
            "You got destroyed so bad it's generational",
            "Bro is the human equivalent of lag",
            "Your gameplay makes me lose brain cells",
            "I've fought training dummies harder than you",
            "You're so bad it's actually impressive",
            "Sit down you just got violated",
            "Your bloodline ends with this L",
            "Bro got demolished and is coping",
            "You fight like a lagging potato",
            "This is just embarrassing for you",
            "Your entire gaming career just ended",
            "You got folded like a lawn chair",
            "Bro is getting absolutely dominated",
            "Your performance was a war crime",
            "I've seen better plays from a rock",
            "You're so bad you need a nerf",
            "Tellurium made you look silly",
            "Your mouse working properly?",
            "Bro got sent to the shadow realm",
            "You're fighting like it's your first day",
            "Tellurium owns you completely",
            "Your connection or your brain lagging?",
            "Absolutely no chance kid",
            "You're cooked bro",
            "Tellurium just ended your career",
            "Your W key stuck or what?",
            "This wasn't even close",
            "You got smoked kid",
            "Tellurium is just superior",
            "Bro playing on a calculator",
            "You're getting violated repeatedly",
            "Your entire setup is questionable",
            "Free elo thanks",
            "You got packed and shipped",
            "Tellurium remains unmatched",
            "Your skills nonexistent bro"
    );

    private final Random random = new Random();
    private long lastKillTime = 0;

    public Killsult() {
        super("Killsult", Category.COMBAT);
        addSettings(mode);
    }

    @Listener
    public void onPacketReceive(PacketReceiveEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (event.getPacket() instanceof S02PacketChat) {
            S02PacketChat packet = (S02PacketChat) event.getPacket();
            String message = packet.getChatComponent().getUnformattedText();

            if (System.currentTimeMillis() - lastKillTime < 1000) return;

            if (message.contains("was killed by " + mc.thePlayer.getName()) ||
                    message.contains("was slain by " + mc.thePlayer.getName()) ||
                    message.contains(mc.thePlayer.getName() + " killed")) {

                sendKillsult();
                lastKillTime = System.currentTimeMillis();
            }
        }
    }

    private void sendKillsult() {
        String message = telluriumLines.get(random.nextInt(telluriumLines.size()));
        mc.thePlayer.sendChatMessage(message);
    }
}