package com.mapacheteam.colas;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = ColasMod.MODID)
public class ColasEvents {

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("colas")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(c -> GameManager.start(c.getSource(), false))
                        .then(Commands.literal("gulag")
                                .executes(c -> GameManager.start(c.getSource(), true))))
                .then(Commands.literal("clear").executes(c -> GameManager.clear(c.getSource())))
                .then(Commands.literal("end").executes(c -> GameManager.end(c.getSource())))
                .then(Commands.literal("give")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(c -> GameManager.give(c.getSource(), EntityArgument.getPlayer(c, "jugador"))))));
    }

    /** Golpe = robar cola. Durante toda la partida (y al terminar) los golpes entre jugadores no hacen dano. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer attacker)) return;
        if (!(e.getTarget() instanceof ServerPlayer victim)) return;
        if (GameManager.state == GameManager.State.IDLE) return;
        e.setCanceled(true);
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s != null) GameManager.trySteal(s, attacker, victim);
    }

    /** Agarrar la cola: click derecho a un bloque "cola" (siendo del team sincola). */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND) return;
        if (GameManager.state != GameManager.State.RUNNING) return;
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!GameManager.isInTeam(p, GameManager.T_SIN)) return;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(e.getLevel().getBlockState(e.getPos()).getBlock());
        if (id == null || !id.getPath().contains(GameManager.TAIL_BLOCK_KEYWORD)) return;
        e.getLevel().removeBlock(e.getPos(), false);
        e.setCanceled(true);
        GameManager.giveTail(p.server, p);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s != null) GameManager.tick(s);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) GameManager.syncHud(p);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        GameManager.reset();
    }
}
