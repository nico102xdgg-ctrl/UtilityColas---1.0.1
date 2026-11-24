package com.mapacheteam.colas;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ColasMod.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void init() {
        CHANNEL.registerMessage(0, HudPacket.class, HudPacket::encode, HudPacket::decode, HudPacket::handle);
    }

    public static void sendHud(ServerPlayer p, int hud, int tint, int ticks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new HudPacket(hud, tint, ticks));
    }
}
