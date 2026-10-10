package com.caja;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * La Caja puesta en el suelo.
 * - Guarda en su BlockEntity quién la colocó.
 * - Cada click derecho sube la propiedad CLICKS (0 -> 1 -> 2).
 * - Al tercer click se destruye, suelta partículas y aparece el Clon.
 */
public class CajaBlock extends Block implements EntityBlock {
    public static final MapCodec<CajaBlock> CODEC = simpleCodec(CajaBlock::new);
    public static final IntegerProperty CLICKS = IntegerProperty.create("clicks", 0, 2);

    public CajaBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CLICKS, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CLICKS);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CajaBlockEntity(pos, state);
    }

    /** Se llama cuando un jugador coloca el bloque: aquí anotamos quién fue. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof CajaBlockEntity be) {
            be.setOwner(player.getUUID(), player.getGameProfile().name());
        }
    }

    /** Click derecho con la mano vacía (o con algo que no se pueda colocar). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // El cliente solo muestra la animación; la lógica real corre en el servidor.
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }

        int clicks = state.getValue(CLICKS) + 1;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;

        if (clicks < 3) {
            // Click 1 y 2: la caja "se sacude".
            server.setBlock(pos, state.setValue(CLICKS, clicks), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0f, 0.8f + clicks * 0.2f);
            server.sendParticles(ParticleTypes.CRIT, x, y + 0.3, z, 6, 0.25, 0.25, 0.25, 0.05);
            return InteractionResult.SUCCESS;
        }

        // Click 3: se rompe y nace el clon.
        UUIDAndName owner = server.getBlockEntity(pos) instanceof CajaBlockEntity be
                ? new UUIDAndName(be.getOwnerUuid(), be.getOwnerName())
                : new UUIDAndName(player.getUUID(), player.getGameProfile().name()); // por si acaso

        server.removeBlock(pos, false);
        server.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        server.sendParticles(ParticleTypes.POOF, x, y, z, 30, 0.35, 0.35, 0.35, 0.05);
        server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 0.5, z, 20, 0.3, 0.5, 0.3, 0.2);

        ClonEntity clon = CajaMod.CLON.create(server, EntitySpawnReason.TRIGGERED);
        if (clon != null) {
            clon.setOwner(owner.uuid(), owner.name());
            clon.setPos(x, pos.getY(), z);
            clon.setYRot(player.getYRot() + 180f);   // mira hacia quien lo abrió
            server.addFreshEntity(clon);             // el servidor le asigna un id nuevo
        }
        return InteractionResult.SUCCESS;
    }

    private record UUIDAndName(java.util.UUID uuid, String name) {}
}
