package com.caja;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * El Clon: una entidad nueva (con su propio id) que se ve como un jugador.
 * - Lleva el nombre y la skin del jugador que puso la Caja.
 * - Vagabundea sola. Click derecho con la mano vacía = te subes y LO CONTROLAS
 *   (WASD para moverte, mouse para girar, saltar para que salte, Shift para bajarte).
 * - Se ajusta con /clon scale|speed|jump (ver ClonCommands).
 */
public class ClonEntity extends PathfinderMob {
    // Dato sincronizado servidor -> clientes: el cliente lo necesita para buscar la skin.
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(ClonEntity.class, EntityDataSerializers.STRING);

    private UUID ownerUuid;

    public ClonEntity(EntityType<? extends ClonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.JUMP_STRENGTH, 0.42)
                .add(Attributes.SCALE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_NAME, "");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    // ---------- dueño ----------
    public void setOwner(UUID uuid, String name) {
        this.ownerUuid = uuid;
        entityData.set(OWNER_NAME, name);
        setCustomName(Component.literal(name));   // mismo nombre que el jugador
        setCustomNameVisible(true);
    }

    public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return entityData.get(OWNER_NAME); }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("OwnerName", getOwnerName());
        if (ownerUuid != null) output.store("OwnerUuid", UUIDUtil.CODEC, ownerUuid);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(OWNER_NAME, input.getStringOr("OwnerName", ""));
        ownerUuid = input.read("OwnerUuid", UUIDUtil.CODEC).orElse(null);
    }

    // ---------- control ----------
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).isEmpty() || isVehicle()) {
            return super.mobInteract(player, hand);
        }
        if (level() instanceof ServerLevel) {
            player.startRiding(this);      // te montas encima y pasas a controlarlo
        }
        return InteractionResult.SUCCESS;
    }

    /** Quién manda sobre el movimiento: el jugador montado. */
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    /** Cada tick con jugador al mando: el clon mira hacia donde mira el jugador. */
    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        setRot(rider.getYRot(), rider.getXRot() * 0.5f);
        yRotO = yBodyRot = yHeadRot = getYRot();
        // Si el jugador salta, el clon salta (la altura depende de /clon jump).
        if (onGround() && rider.getDeltaMovement().y > 0.1) {
            jumpFromGround();
        }
    }

    /** WASD del jugador -> movimiento del clon. */
    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 travelVector) {
        float side = rider.xxa * 0.5f;
        float forward = rider.zza;
        if (forward <= 0f) forward *= 0.5f;   // ir hacia atrás es más lento
        return new Vec3(side, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);   // /clon speed
    }
}
