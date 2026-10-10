package com.caja.client;

import com.caja.CajaMod;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

/** Solo se ejecuta en el cliente (ventana del juego): le dice cómo DIBUJAR al clon. */
public class CajaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRenderers.register(CajaMod.CLON, ClonRenderer::new);
    }
}
