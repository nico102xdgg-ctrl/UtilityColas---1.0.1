package com.mapacheteam.colas;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ColasMod.MODID)
public class ColasMod {
    public static final String MODID = "colas";

    public ColasMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModSounds.SOUNDS.register(bus);
        ModParticles.PARTICLES.register(bus);
        ModNetwork.init();
    }
}
