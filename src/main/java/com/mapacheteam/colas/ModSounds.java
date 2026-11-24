package com.mapacheteam.colas;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ColasMod.MODID);

    public static final RegistryObject<SoundEvent> COLAROBAR = reg("colarobar");
    public static final RegistryObject<SoundEvent> COLAROBADA = reg("colarobada");
    public static final RegistryObject<SoundEvent> START_SHOOT = reg("start_shoot");
    public static final RegistryObject<SoundEvent> COLASEND = reg("colasend");
    public static final RegistryObject<SoundEvent> COLAS = reg("colas");
    public static final RegistryObject<SoundEvent> COLASGULAG = reg("colasgulag");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () ->
                SoundEvent.createVariableRangeEvent(new ResourceLocation(ColasMod.MODID, name)));
    }
}
