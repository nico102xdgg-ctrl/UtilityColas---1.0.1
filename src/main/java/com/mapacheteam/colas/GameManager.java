package com.mapacheteam.colas;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GameManager {

    public enum State { IDLE, COUNTDOWN, RUNNING, ENDED }

    // ====== AJUSTES ======
    public static final String T_JUGADOR = "jugador";
    public static final String T_CON = "concola";
    public static final String T_SIN = "sincola";

    /** Un bloque cuyo id contenga este texto (ej. waterblocks:cola) se agarra con click derecho. */
    public static final String TAIL_BLOCK_KEYWORD = "cola";

    public static final int COUNTDOWN_TICKS = 10 * 20;
    public static final int GAME_TICKS = 10 * 60 * 20;
    public static final int GULAG_TICKS = 5 * 60 * 20;
    public static final int GLOW_TICKS = 15 * 20;
    public static final int TINT_TICKS = 15 * 20;
    /** Tiempo en el que no se puede volver a robar a alguien recien robado (evita doble golpe). */
    public static final int STEAL_PROTECTION_TICKS = 20;

    // Comandos de otros mods (se ejecutan como consola). Cambialos aqui si tu mod de timers usa otra sintaxis.
    public static final String CMD_TIMER_SET = "stimer set %s %s";   // (tiempo, nombre)
    public static final String CMD_TIMER_STOP = "stimer stop %s";    // (nombre)
    // =====================

    public static State state = State.IDLE;
    public static boolean gulag = false;
    private static int countdown;
    private static int ticksLeft;
    private static final Map<UUID, Integer> protectedUntil = new HashMap<>();

    private GameManager() {}

    // ---------- utilidades ----------
    public static void run(MinecraftServer s, String cmd) {
        s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), cmd);
    }

    private static PlayerTeam team(MinecraftServer s, String name) {
        Scoreboard sb = s.getScoreboard();
        PlayerTeam t = sb.getPlayerTeam(name);
        if (t == null) {
            t = sb.addPlayerTeam(name);
            t.setAllowFriendlyFire(true);
            if (name.equals(T_CON)) t.setColor(ChatFormatting.YELLOW);
            if (name.equals(T_SIN)) t.setColor(ChatFormatting.RED);
        }
        return t;
    }

    private static void setTeam(MinecraftServer s, ServerPlayer p, String name) {
        s.getScoreboard().addPlayerToTeam(p.getScoreboardName(), team(s, name));
    }

    public static boolean isInTeam(ServerPlayer p, String name) {
        Team t = p.getTeam();
        return t != null && t.getName().equals(name);
    }

    private static String timerName() { return gulag ? "colasgulag" : "colas"; }

    public static void play(ServerPlayer p, SoundEvent ev, SoundSource src) {
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ev);
        p.connection.send(new ClientboundSoundEntityPacket(holder, src, p, 1.0F, 1.0F, p.getRandom().nextLong()));
    }

    private static void stopSounds(MinecraftServer s) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundStopSoundPacket(ModSounds.COLAS.getId(), null));
            p.connection.send(new ClientboundStopSoundPacket(ModSounds.COLASGULAG.getId(), null));
            p.connection.send(new ClientboundStopSoundPacket(ModSounds.COLASEND.getId(), null));
            p.connection.send(new ClientboundStopSoundPacket(ModSounds.START_SHOOT.getId(), null));
        }
    }

    private static void stopMusic(MinecraftServer s) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundStopSoundPacket(ModSounds.COLAS.getId(), null));
            p.connection.send(new ClientboundStopSoundPacket(ModSounds.COLASGULAG.getId(), null));
        }
    }

    // ---------- cola ----------
    public static void giveTail(MinecraftServer s, ServerPlayer p) {
        setTeam(s, p, T_CON);
        p.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false));
        ModNetwork.sendHud(p, 1, 1, TINT_TICKS);
        run(s, "anim setlayer " + p.getScoreboardName() + " cola");
    }

    private static void loseTail(MinecraftServer s, ServerPlayer p) {
        setTeam(s, p, T_SIN);
        p.removeEffect(MobEffects.GLOWING);
        ModNetwork.sendHud(p, 2, 2, TINT_TICKS);
        run(s, "anim setlayer " + p.getScoreboardName() + " traje");
    }

    public static void trySteal(MinecraftServer s, ServerPlayer attacker, ServerPlayer victim) {
        if (state != State.RUNNING) return;
        if (!isInTeam(attacker, T_SIN) || !isInTeam(victim, T_CON)) return;
        int now = s.getTickCount();
        if (protectedUntil.getOrDefault(victim.getUUID(), 0) > now) return;

        loseTail(s, victim);
        giveTail(s, attacker);
        protectedUntil.put(attacker.getUUID(), now + STEAL_PROTECTION_TICKS);

        play(attacker, ModSounds.COLAROBAR.get(), SoundSource.PLAYERS);
        play(victim, ModSounds.COLAROBADA.get(), SoundSource.PLAYERS);

        ((ServerLevel) victim.level()).sendParticles(ModParticles.COLA_STAR.get(),
                victim.getX(), victim.getY() + 1.0, victim.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
    }

    // ---------- comandos ----------
    public static int start(CommandSourceStack src, boolean isGulag) {
        MinecraftServer s = src.getServer();
        if (state == State.COUNTDOWN || state == State.RUNNING) {
            src.sendFailure(Component.literal("Ya hay una partida en curso. Usa /colas end o /colas clear."));
            return 0;
        }
        if (state == State.ENDED) run(s, "unfreeze @a");
        gulag = isGulag;
        protectedUntil.clear();
        stopSounds(s);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (p.isSpectator()) continue;
            setTeam(s, p, T_SIN);
            p.removeEffect(MobEffects.GLOWING);
            run(s, "anim setlayer " + p.getScoreboardName() + " traje");
            ModNetwork.sendHud(p, 0, 0, 0);
        }
        state = State.COUNTDOWN;
        countdown = COUNTDOWN_TICKS;
        src.sendSuccess(() -> Component.literal("Colas" + (isGulag ? " (gulag)" : "") + ": inicia en 10 segundos."), true);
        return 1;
    }

    private static void begin(MinecraftServer s) {
        state = State.RUNNING;
        ticksLeft = gulag ? GULAG_TICKS : GAME_TICKS;
        SoundEvent music = (gulag ? ModSounds.COLASGULAG : ModSounds.COLAS).get();
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            play(p, ModSounds.START_SHOOT.get(), SoundSource.PLAYERS);
            play(p, music, SoundSource.MUSIC);
            if (isInTeam(p, T_SIN)) ModNetwork.sendHud(p, 2, 0, 0);
        }
        run(s, String.format(CMD_TIMER_SET, gulag ? "00:05:00" : "00:10:00", timerName()));
    }

    public static int end(CommandSourceStack src) {
        if (state != State.COUNTDOWN && state != State.RUNNING) {
            src.sendFailure(Component.literal("No hay ninguna partida en curso."));
            return 0;
        }
        finish(src.getServer(), true);
        src.sendSuccess(() -> Component.literal("Colas: partida terminada."), true);
        return 1;
    }

    private static void finish(MinecraftServer s, boolean manual) {
        state = State.ENDED;
        stopMusic(s);
        if (manual) run(s, String.format(CMD_TIMER_STOP, timerName()));
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            play(p, ModSounds.COLASEND.get(), SoundSource.PLAYERS);
            ModNetwork.sendHud(p, -1, 0, 0); // quita solo la imagen roja/amarilla, la HUD se queda
        }
        run(s, "freeze @a[team=" + T_SIN + "]");
        run(s, "effect clear @a[team=" + T_CON + "] minecraft:glowing");
    }

    public static int clear(CommandSourceStack src) {
        MinecraftServer s = src.getServer();
        boolean wasActive = state == State.COUNTDOWN || state == State.RUNNING;
        state = State.IDLE;
        protectedUntil.clear();
        stopSounds(s);
        if (wasActive) run(s, String.format(CMD_TIMER_STOP, timerName()));

        Scoreboard sb = s.getScoreboard();
        PlayerTeam jugador = team(s, T_JUGADOR);
        for (String teamName : new String[]{T_CON, T_SIN}) {
            PlayerTeam t = team(s, teamName);
            for (String name : new ArrayList<>(t.getPlayers())) sb.addPlayerToTeam(name, jugador);
        }
        run(s, "unfreeze @a");
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.removeEffect(MobEffects.GLOWING);
            ModNetwork.sendHud(p, 0, 0, 0);
        }
        run(s, "anim setlayer @a traje");
        src.sendSuccess(() -> Component.literal("Colas: todo limpiado."), true);
        return 1;
    }

    public static int give(CommandSourceStack src, ServerPlayer target) {
        if (state != State.RUNNING) {
            src.sendFailure(Component.literal("Solo se puede dar cola mientras la partida esta en curso."));
            return 0;
        }
        giveTail(src.getServer(), target);
        return 1;
    }

    // ---------- tick / login ----------
    public static void tick(MinecraftServer s) {
        if (state == State.COUNTDOWN) {
            if (countdown > 0 && countdown % 20 == 0) {
                Component num = Component.literal(String.valueOf(countdown / 20)).withStyle(ChatFormatting.YELLOW);
                for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                    p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 25, 5));
                    p.connection.send(new ClientboundSetTitleTextPacket(num));
                }
            }
            if (--countdown <= 0) begin(s);
        } else if (state == State.RUNNING) {
            if (--ticksLeft <= 0) finish(s, false);
        }
    }

    public static void syncHud(ServerPlayer p) {
        int hud = 0;
        if (state == State.RUNNING || state == State.ENDED) {
            if (isInTeam(p, T_CON)) hud = 1;
            else if (isInTeam(p, T_SIN)) hud = 2;
        }
        ModNetwork.sendHud(p, hud, 0, 0);
    }

    public static void reset() {
        state = State.IDLE;
        protectedUntil.clear();
    }
}
