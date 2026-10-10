package com.caja;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.List;

/**
 * /clon scale <valor>   tamaño (1 = normal)
 * /clon speed <valor>   velocidad (0.25 = normal)
 * /clon jump  <valor>   fuerza de salto (0.42 = normal)
 * /clon reset           todo a valores normales
 * Afectan a TODOS los clones que pertenecen a quien ejecuta el comando.
 */
public final class ClonCommands {
    private ClonCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("clon")
                .then(Commands.literal("scale").then(Commands.argument("valor", DoubleArgumentType.doubleArg(0.0625, 16))
                        .executes(c -> set(c, Attributes.SCALE, "tamaño"))))
                .then(Commands.literal("speed").then(Commands.argument("valor", DoubleArgumentType.doubleArg(0, 5))
                        .executes(c -> set(c, Attributes.MOVEMENT_SPEED, "velocidad"))))
                .then(Commands.literal("jump").then(Commands.argument("valor", DoubleArgumentType.doubleArg(0, 4))
                        .executes(c -> set(c, Attributes.JUMP_STRENGTH, "salto"))))
                .then(Commands.literal("reset").executes(ClonCommands::reset)));
    }

    private static List<? extends ClonEntity> myClones(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        return c.getSource().getLevel().getEntities(
                EntityTypeTest.forClass(ClonEntity.class),
                clon -> player.getUUID().equals(clon.getOwnerUuid()));
    }

    private static int set(CommandContext<CommandSourceStack> c, Holder<Attribute> attr, String label) throws CommandSyntaxException {
        double value = DoubleArgumentType.getDouble(c, "valor");
        List<? extends ClonEntity> clones = myClones(c);
        clones.forEach(clon -> clon.getAttribute(attr).setBaseValue(value));
        c.getSource().sendSuccess(() -> Component.literal("Clones afectados: " + clones.size() + " (" + label + " = " + value + ")"), false);
        return clones.size();
    }

    private static int reset(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        List<? extends ClonEntity> clones = myClones(c);
        clones.forEach(clon -> {
            clon.getAttribute(Attributes.SCALE).setBaseValue(1.0);
            clon.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25);
            clon.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.42);
        });
        c.getSource().sendSuccess(() -> Component.literal("Clones reiniciados: " + clones.size()), false);
        return clones.size();
    }
}
