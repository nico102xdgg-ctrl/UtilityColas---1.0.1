package com.mapacheteam.colas;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ColasMod.MODID);

    public static final RegistryObject<SimpleParticleType> COLA_STAR =
            PARTICLES.register("cola_star", () -> new SimpleParticleType(true));
}
