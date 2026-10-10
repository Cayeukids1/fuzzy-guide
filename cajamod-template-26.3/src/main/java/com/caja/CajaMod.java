package com.caja;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Punto de entrada del mod. Aquí REGISTRAMOS todo lo nuevo que añadimos al juego:
 * un bloque (la Caja puesta en el suelo), su ítem, su "block entity" (guarda quién la puso)
 * y la entidad Clon (el "jugador" que sale al romperla).
 */
public class CajaMod implements ModInitializer {
    public static final String MOD_ID = "cajamod";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // Cada cosa registrada necesita una "llave" (ResourceKey) con su id.
    private static final ResourceKey<Block> CAJA_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, id("caja"));
    private static final ResourceKey<Item> CAJA_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("caja"));
    private static final ResourceKey<EntityType<?>> CLON_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("clon"));

    public static final Block CAJA_BLOCK = Registry.register(BuiltInRegistries.BLOCK, CAJA_BLOCK_KEY,
            new CajaBlock(BlockBehaviour.Properties.of()
                    .strength(1.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .setId(CAJA_BLOCK_KEY)));

    public static final Item CAJA_ITEM = Registry.register(BuiltInRegistries.ITEM, CAJA_ITEM_KEY,
            new BlockItem(CAJA_BLOCK, new Item.Properties()
                    .setId(CAJA_ITEM_KEY)
                    .useBlockDescriptionPrefix()));   // el nombre sale de "block.cajamod.caja" => "Caja"

    public static final BlockEntityType<CajaBlockEntity> CAJA_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("caja"),
            FabricBlockEntityTypeBuilder.create(CajaBlockEntity::new, CAJA_BLOCK).build());

    public static final EntityType<ClonEntity> CLON = Registry.register(BuiltInRegistries.ENTITY_TYPE, CLON_KEY,
            EntityType.Builder.of(ClonEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)        // mismo tamaño que un jugador
                    .eyeHeight(1.62f)
                    .build(CLON_KEY));

    @Override
    public void onInitialize() {
        // Atributos base del clon (vida, velocidad, salto, tamaño)
        FabricDefaultAttributeRegistry.register(CLON, ClonEntity.createAttributes());
        // La Caja aparece en la pestaña creativa "Bloques funcionales"
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(CAJA_ITEM));
        // Comandos /clon ...
        CommandRegistrationCallback.EVENT.register((dispatcher, ctx, env) -> ClonCommands.register(dispatcher));
    }
}
