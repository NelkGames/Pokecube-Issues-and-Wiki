package pokecube.core.client.render.mobs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.w3c.dom.Node;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import pokecube.core.PokecubeCore;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.entity.pokemobs.PokemobType;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.interfaces.pokemob.ai.LogicStates;
import pokecube.core.moves.MovesUtils;
import thut.api.entity.IMobTexturable;
import thut.api.maths.Vector3;
import thut.core.client.render.animation.Animation;
import thut.core.client.render.animation.AnimationLoader;
import thut.core.client.render.animation.IAnimationChanger;
import thut.core.client.render.animation.ModelHolder;
import thut.core.client.render.model.IExtendedModelPart;
import thut.core.client.render.model.IModel;
import thut.core.client.render.model.IModelRenderer;
import thut.core.client.render.model.ModelFactory;
import thut.core.client.render.model.PartInfo;
import thut.core.client.render.texturing.IPartTexturer;
import thut.core.client.render.texturing.TextureHelper;
import thut.core.client.render.wrappers.ModelWrapper;
import thut.core.common.ThutCore;

public class RenderPokemob extends MobRenderer<TameableEntity, ModelWrapper<TameableEntity>>
{
    public static class Holder extends ModelHolder implements IModelRenderer<TameableEntity>
    {
        ModelWrapper<TameableEntity>            wrapper;
        final Vector3                           rotPoint                  = Vector3.getNewVector();
        HashMap<String, List<Animation>>        anims                     = Maps.newHashMap();
        private IPartTexturer                   texturer;
        private IAnimationChanger               animator;
        public String                           name;
        public HashMap<String, PartInfo>        parts                     = Maps.newHashMap();
        HashMap<String, ArrayList<Vector5>>     global;
        public HashMap<String, List<Animation>> animations                = Maps.newHashMap();
        public Vector3                          offset                    = Vector3.getNewVector();;
        public Vector3                          scale                     = Vector3.getNewVector();
        ResourceLocation                        texture;
        PokedexEntry                            entry;
        // Used to check if it has a custom sleeping animation.
        private boolean                         checkedForContactAttack   = false;
        private boolean                         hasContactAttackAnimation = false;

        // Used to check if it has a custom sleeping animation.
        private boolean                         checkedForRangedAttack    = false;
        private boolean                         hasRangedAttackAnimation  = false;

        public boolean                          overrideAnim              = false;
        public String                           anim                      = "";

        public Vector5                          rotations                 = new Vector5();

        boolean                                 blend;

        boolean                                 light;

        int                                     src;

        ///////////////////// IModelRenderer stuff below here//////////////////

        int                                     dst;

        public Holder(final PokedexEntry entry)
        {
            super(entry.model(), entry.texture(), entry.animation(), entry.getTrimmedName());
            this.entry = entry;
        }

        @Override
        public void doRender(final TameableEntity entity, final double d, final double d1, final double d2,
                final float f, final float partialTick)
        {
        }

        @Override
        public String getAnimation(final Entity entityIn)
        {
            return this.getPhase((MobEntity) entityIn, CapabilityPokemob.getPokemobFor(entityIn));
        }

        @Override
        public IAnimationChanger getAnimationChanger()
        {
            return this.animator;
        }

        @Override
        public HashMap<String, List<Animation>> getAnimations()
        {
            return this.animations;
        }

        private HashMap<String, PartInfo> getChildren(final IExtendedModelPart part)
        {
            final HashMap<String, PartInfo> partsList = new HashMap<>();
            for (final String s : part.getSubParts().keySet())
            {
                final PartInfo p = new PartInfo(s);
                final IExtendedModelPart subPart = (IExtendedModelPart) part.getSubParts().get(s);
                p.children = this.getChildren(subPart);
                partsList.put(s, p);
            }
            return partsList;
        }

        private PartInfo getPartInfo(final String partName)
        {
            PartInfo ret = null;
            for (final PartInfo part : this.parts.values())
            {
                if (part.name.equalsIgnoreCase(partName)) return part;
                ret = this.getPartInfo(partName, part);
                if (ret != null) return ret;
            }
            for (final IExtendedModelPart part : this.wrapper.getParts().values())
                if (part.getName().equals(partName))
                {
                    final PartInfo p = new PartInfo(part.getName());
                    p.children = this.getChildren(part);
                    boolean toAdd = true;
                    IExtendedModelPart parent = part.getParent();
                    while (parent != null && toAdd)
                    {
                        toAdd = !this.parts.containsKey(parent.getName());
                        parent = parent.getParent();
                    }
                    if (toAdd) this.parts.put(partName, p);
                    return p;
                }

            return ret;
        }

        private PartInfo getPartInfo(final String partName, final PartInfo parent)
        {
            PartInfo ret = null;
            for (final PartInfo part : parent.children.values())
            {
                if (part.name.equalsIgnoreCase(partName)) return part;
                ret = this.getPartInfo(partName, part);
                if (ret != null) return ret;
            }

            return ret;
        }

        private String getPhase(final MobEntity entity, final IPokemob pokemob)
        {
            String phase = IModelRenderer.super.getAnimation(entity);
            if (this.model == null || pokemob == null) return phase;
            final Vec3d velocity = entity.getMotion();
            final float dStep = entity.limbSwingAmount - entity.prevLimbSwingAmount;
            final float walkspeed = (float) (velocity.x * velocity.x + velocity.z * velocity.z + dStep * dStep);
            final float stationary = 0.00001f;
            final boolean asleep = pokemob.getStatus() == IMoveConstants.STATUS_SLP
                    || pokemob.getLogicState(LogicStates.SLEEPING);

            if (!this.checkedForContactAttack)
            {
                this.hasContactAttackAnimation = this.hasAnimation("attack_contact", entity);
                this.checkedForContactAttack = true;
            }
            if (!this.checkedForRangedAttack)
            {
                this.hasRangedAttackAnimation = this.hasAnimation("attack_ranged", entity);
                this.checkedForRangedAttack = true;
            }
            if (pokemob.getCombatState(CombatStates.EXECUTINGMOVE))
            {
                final int index = pokemob.getMoveIndex();
                Move_Base move;
                if (index < 4 && (move = MovesUtils.getMoveFromName(pokemob.getMove(index))) != null)
                {
                    if (this.hasContactAttackAnimation
                            && (move.getAttackCategory() & IMoveConstants.CATEGORY_CONTACT) > 0)
                    {
                        phase = "attack_contact";
                        return phase;
                    }
                    if (this.hasRangedAttackAnimation
                            && (move.getAttackCategory() & IMoveConstants.CATEGORY_DISTANCE) > 0)
                    {
                        phase = "attack_ranged";
                        return phase;
                    }
                }
            }

            for (final LogicStates state : LogicStates.values())
            {
                final String anim = ThutCore.trim(state.toString());
                if (pokemob.getLogicState(state) && this.hasAnimation(anim, entity)) return anim;
            }

            if (asleep && this.hasAnimation("sleeping", entity))
            {
                phase = "sleeping";
                return phase;
            }
            if (asleep && this.hasAnimation("asleep", entity))
            {
                phase = "asleep";
                return phase;
            }
            if (!entity.onGround && this.hasAnimation("flight", entity))
            {
                phase = "flight";
                return phase;
            }
            if (!entity.onGround && this.hasAnimation("flying", entity))
            {
                phase = "flying";
                return phase;
            }
            if (entity.isInWater() && this.hasAnimation("swimming", entity))
            {
                phase = "swimming";
                return phase;
            }
            if (entity.onGround && walkspeed > stationary && this.hasAnimation("walking", entity))
            {
                phase = "walking";
                return phase;
            }
            if (entity.onGround && walkspeed > stationary && this.hasAnimation("walk", entity))
            {
                phase = "walk";
                return phase;
            }

            for (final CombatStates state : CombatStates.values())
            {
                final String anim = ThutCore.trim(state.toString());
                if (pokemob.getCombatState(state) && this.hasAnimation(anim, entity)) return anim;
            }
            return phase;
        }

        @Override
        public Vector3 getRotationOffset()
        {
            return this.offset;
        }

        @Override
        public Vector5 getRotations()
        {
            return this.rotations;
        }

        @Override
        public Vector3 getScale()
        {
            return this.scale;
        }

        @Override
        public IPartTexturer getTexturer()
        {
            return this.texturer;
        }

        @Override
        public void handleCustomTextures(final Node node)
        {
            this.setTextureDetails(node);
        }

        @Override
        public boolean hasAnimation(final String phase, final Entity entity)
        {
            return IModelRenderer.DEFAULTPHASE.equals(phase) || this.animations.containsKey(phase)
                    || this.wrapper.imodel.getBuiltInAnimations().contains(phase);
        }

        public void init()
        {
            this.initModel(new ModelWrapper<>(this, this));
        }

        public void initModel(final ModelWrapper<TameableEntity> model)
        {
            this.wrapper = model;
            this.name = model.model.name;
            this.texture = model.model.texture;
            model.imodel = ModelFactory.create(model.model);

            // Check if an animation file exists.
            try
            {
                Minecraft.getInstance().getResourceManager().getResource(this.animation);
            }
            catch (final IOException e)
            {
                // No animation here, lets try to use the base one.
            }

            AnimationLoader.parse(model.model, model, this);
            this.initModelParts();
        }

        private void initModelParts()
        {
            if (this.wrapper == null) return;

            for (final String s : this.wrapper.getParts().keySet())
                if (this.wrapper.getParts().get(s).getParent() == null && !this.parts.containsKey(s))
                {
                    final PartInfo p = this.getPartInfo(s);
                    this.parts.put(s, p);
                }
        }

        @Override
        public void scaleEntity(final MatrixStack mat, final Entity entity, final IModel model, final float partialTick)
        {
            final IPokemob pokemob = CapabilityPokemob.getPokemobFor(entity);
            float s = 1;
            if (pokemob != null) s = pokemob.getEntity().getRenderScale();
            float sx = (float) this.getScale().x;
            float sy = (float) this.getScale().y;
            float sz = (float) this.getScale().z;
            sx *= s;
            sy *= s;
            sz *= s;
            this.rotPoint.set(this.getRotationOffset()).scalarMultBy(s);
            model.setOffset(this.rotPoint);
            if (!this.getScale().isEmpty()) mat.scale(sx, sy, sz);
            else mat.scale(s, s, s);
        }

        @Override
        public void setAnimationChanger(final IAnimationChanger changer)
        {
            this.animator = changer;
        }

        @Override
        public void setRotationOffset(final Vector3 offset)
        {
            this.offset = offset;
        }

        @Override
        public void setRotations(final Vector5 rotations)
        {
            this.rotations = rotations;
        }

        @Override
        public void setScale(final Vector3 scale)
        {
            this.scale = scale;
        }

        private void setTextureDetails(final Node node)
        {
            if (node.getAttributes() == null) return;
            String[] male = null, female = null;
            if (node.getAttributes().getNamedItem("male") != null)
            {
                String shift;
                shift = node.getAttributes().getNamedItem("male").getNodeValue();
                male = shift.split(",");
                for (int i = 0; i < male.length; i++)
                    male[i] = Database.trim(male[i]);
            }
            if (node.getAttributes().getNamedItem("female") != null)
            {
                String shift;
                shift = node.getAttributes().getNamedItem("female").getNodeValue();
                female = shift.split(",");
                for (int i = 0; i < female.length; i++)
                    female[i] = Database.trim(female[i]);
            }
            if (female == null && male != null || this.entry.textureDetails == null) female = male;
            if (male != null)
            {
                this.entry.textureDetails[0] = male;
                this.entry.textureDetails[1] = female;
            }
        }

        @Override
        public void setTexturer(final IPartTexturer texturer)
        {
            this.texturer = texturer;
        }

        @Override
        public void updateModel(final HashMap<String, ArrayList<Vector5>> global, final ModelHolder model)
        {
            this.name = model.name;
            this.texture = model.texture;
            this.initModelParts();
            this.global = global;
        }
    }

    public static boolean                     reload_models = false;

    public static Map<PokemobType<?>, Holder> holderMap     = Maps.newHashMap();

    public static void register()
    {
        PokecubeCore.LOGGER.info("Registering Models to the renderer.");
        for (final PokedexEntry entry : Database.getSortedFormes())
        {
            final PokemobType<?> type = (PokemobType<?>) PokecubeCore.typeMap.get(entry);
            RenderPokemob.holderMap.put(type, new Holder(entry));
        }
    }

    private static Holder MISSNGNO = new Holder(Database.missingno);

    private static Holder getMissingNo()
    {
        if (RenderPokemob.MISSNGNO.wrapper == null) RenderPokemob.MISSNGNO.init();
        return RenderPokemob.MISSNGNO;
    }

    final Holder holder;

    public RenderPokemob(final PokedexEntry entry, final EntityRendererManager p_i50961_1_)
    {
        super(p_i50961_1_, null, 1);
        this.holder = new Holder(entry);
    }

    @Override
    public void render(final TameableEntity entity, final float entityYaw, final float partialTicks,
            final MatrixStack matrixStackIn, final IRenderTypeBuffer bufferIn, final int packedLightIn)
    {
        final PokemobType<?> type = (PokemobType<?>) entity.getType();
        Holder holder = this.holder;
        if (holder.wrapper == null || RenderPokemob.reload_models)
        {
            RenderPokemob.reload_models = false;
            holder.init();
            PokecubeMod.LOGGER.info("Reloaded model for " + type.getEntry());
        }
        if (holder.wrapper == null || holder.wrapper.imodel == null || !holder.wrapper.isValid()
                || holder.entry != type.getEntry() || holder.model == null || holder.texture == null)
            holder = RenderPokemob.getMissingNo();
        this.entityModel = holder.wrapper;
        this.shadowSize = entity.getWidth();
        try
        {
            final IPartTexturer texer = holder.wrapper.renderer.getTexturer();
            final ResourceLocation default_ = this.getEntityTexture(entity);
            if (texer != null)
            {
                texer.bindObject(entity);
                holder.wrapper.getParts().forEach((n, p) ->
                {
                    // Get the default texture for this part.
                    final ResourceLocation tex_part = texer.getTexture(n, default_);
                    p.applyTexture(bufferIn, tex_part, texer);
                });
            }
            super.render(entity, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        }
        catch (final Exception e)
        {
            // holderMap.put(type, this.holder);
            PokecubeCore.LOGGER.error("Error rendering " + type.getEntry(), e);
        }
    }

    @Override
    protected RenderType func_230042_a_(final TameableEntity entity, final boolean bool_a, final boolean bool_b)
    {
        final RenderType.State rendertype$state = RenderType.State.builder()
                .texture(new RenderState.TextureState(this.getEntityTexture(entity), false, false))
                .transparency(new RenderState.TransparencyState("translucent_transparency", () ->
                {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                }, () ->
                {
                    RenderSystem.disableBlend();
                })).diffuseLighting(new RenderState.DiffuseLightingState(true))
                .alpha(new RenderState.AlphaState(0.003921569F)).cull(new RenderState.CullState(false))
                .lightmap(new RenderState.LightmapState(true)).overlay(new RenderState.OverlayState(true)).build(false);
        return RenderType.get("pokecube:pokemob", DefaultVertexFormats.ITEM, GL11.GL_TRIANGLES, 256, bool_a, bool_b,
                rendertype$state);
    }

    @Override
    public ResourceLocation getEntityTexture(final TameableEntity entity)
    {
        final IMobTexturable mob = entity.getCapability(TextureHelper.CAPABILITY).orElse(null);
        final ResourceLocation texture = RenderPokemob.getMissingNo().texture;
        if (mob != null) return mob.getTexture(null);
        return texture;
    }
}
