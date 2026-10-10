package com.caja.client;

import com.caja.ClonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

/**
 * Dibuja al clon con el MODELO de jugador y la SKIN de su dueño.
 * Truco: el clon no es un jugador, así que buscamos la skin en la lista de jugadores
 * conectados (la de la tecla Tab). Si el dueño no está conectado, usa una skin por defecto.
 */
public class ClonRenderer extends LivingEntityRenderer<ClonEntity, AvatarRenderState, PlayerModel> {

    public ClonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(ClonEntity clon, AvatarRenderState state, float partialTick) {
        super.extractRenderState(clon, state, partialTick);
        PlayerInfo info = null;
        if (Minecraft.getInstance().getConnection() != null) {
            info = Minecraft.getInstance().getConnection().getPlayerInfo(clon.getOwnerName());
        }
        state.skin = info != null ? info.getSkin() : DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin.body().texturePath();
    }
}
