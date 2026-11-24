package com.mapacheteam.colas;

import com.mapacheteam.colas.client.ClientHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * hud : -1 sin cambios, 0 ninguna, 1 colatrue, 2 colafalse
 * tint: -1 sin cambios, 0 quitar, 1 amarillo, 2 rojo   (ticks = duracion de la imagen)
 */
public record HudPacket(int hud, int tint, int ticks) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(hud);
        buf.writeInt(tint);
        buf.writeInt(ticks);
    }

    public static HudPacket decode(FriendlyByteBuf buf) {
        return new HudPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(HudPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHud.apply(msg.hud, msg.tint, msg.ticks)));
        ctx.get().setPacketHandled(true);
    }
}
