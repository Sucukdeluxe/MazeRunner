import gearth.extensions.ExtensionBase;
import gearth.extensions.parsers.HEntityUpdate;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

public class MazeRunnerRepro {
    public static void main(String[] args) {
        int failures = 0;

        try {
            HEntityUpdate.parse(craftMalformedUserUpdate());
            System.out.println("A: stock HEntityUpdate.parse RETURNED without throwing - crash NOT reproduced");
            failures++;
        } catch (Throwable t) {
            System.out.println("A: stock HEntityUpdate.parse THREW " + t + "  [this is the live crash]");
        }

        ExtensionBase.MessageListener inUserUpdateLike = m -> HEntityUpdate.parse(m.getPacket());

        try {
            inUserUpdateLike.act(new HMessage(craftMalformedUserUpdate(), HMessage.Direction.TOCLIENT, 0));
            System.out.println("B: UNGUARDED listener returned without throwing - unexpected");
            failures++;
        } catch (Throwable t) {
            System.out.println("B: UNGUARDED listener PROPAGATED " + t + "  [confirms the guard is needed]");
        }

        ExtensionBase.MessageListener guarded = InterceptGuard.guard(inUserUpdateLike);
        try {
            guarded.act(new HMessage(craftMalformedUserUpdate(), HMessage.Direction.TOCLIENT, 0));
            System.out.println("C: GUARDED listener - NO PROPAGATION  [PASS]");
        } catch (Throwable t) {
            System.out.println("C: GUARDED listener PROPAGATED " + t + "  [FAIL]");
            failures++;
        }

        System.out.println(failures == 0 ? "RESULT: PASS" : "RESULT: FAIL");
        if (failures != 0) System.exit(1);
    }

    static HPacket craftMalformedUserUpdate() {
        HPacket p = new HPacket(0);
        p.appendInt(1);
        p.appendInt(5);
        p.appendInt(2);
        p.appendInt(3);
        return p;
    }
}
