package com.caja;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/** Datos extra de la Caja: quién la puso (se guarda en el mundo y sobrevive a reinicios). */
public class CajaBlockEntity extends BlockEntity {
    private UUID ownerUuid;
    private String ownerName = "";

    public CajaBlockEntity(BlockPos pos, BlockState state) {
        super(CajaMod.CAJA_BE, pos, state);
    }

    public void setOwner(UUID uuid, String name) {
        this.ownerUuid = uuid;
        this.ownerName = name;
        setChanged();   // avisa al juego de que hay que guardar este bloque
    }

    public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("OwnerName", ownerName);
        if (ownerUuid != null) output.store("OwnerUuid", UUIDUtil.CODEC, ownerUuid);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ownerName = input.getStringOr("OwnerName", "");
        ownerUuid = input.read("OwnerUuid", UUIDUtil.CODEC).orElse(null);
    }
}
