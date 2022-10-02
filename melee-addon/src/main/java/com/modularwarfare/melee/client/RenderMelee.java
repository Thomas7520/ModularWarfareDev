package com.modularwarfare.melee.client;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderer;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.melee.client.animation.AnimationMeleeController;
import com.modularwarfare.melee.client.configs.MeleeRenderConfig;
import com.modularwarfare.melee.common.melee.ItemMelee;
import com.modularwarfare.melee.common.melee.MeleeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;

import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.*;

public class RenderMelee extends CustomItemRenderer {

    private static final String[] LEFT_HAND_PART=new String[]{
            "leftArmModel", "leftArmLayerModel"
    };
    private static final String[] LEFT_SLIM_HAND_PART=new String[]{
            "leftArmSlimModel", "leftArmLayerSlimModel"
    };
    private static final String[] RIGHT_HAND_PART=new String[]{
            "rightArmModel", "rightArmLayerModel"
    };
    private static final String[] RIGHT_SLIM_HAND_PART=new String[]{
            "rightArmSlimModel", "rightArmLayerSlimModel"
    };

    public static final float PI = 3.14159265f;

    private Timer timer;

    public FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);

    public static AnimationMeleeController controller;

    public void renderItem(CustomItemRenderType type, EnumHand hand, ItemStack item, Object... data) {
        if (!(item.getItem() instanceof ItemMelee))
            return;

        MeleeType meleeType = ((ItemMelee) item.getItem()).type;
        if (meleeType == null)
            return;

        if (this.timer == null) {
            this.timer = ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "timer", "field_71428_T");
        }
        float partialTicks = this.timer.renderPartialTicks;

        EnhancedModel model = meleeType.enhancedModel;

        MeleeRenderConfig config = (MeleeRenderConfig) model.config;
        if(this.controller == null || this.controller.getConfig() != config){
            this.controller = new AnimationMeleeController(config);
        }

        EntityPlayerSP player = (EntityPlayerSP) Minecraft.getMinecraft().getRenderViewEntity();

        Matrix4f mat = new Matrix4f();

        GlStateManager.pushMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();

        /**
         * DEFAULT TRANSFORM
         * */
        //mat.translate(new Vector3f(0,1.3f,-1.8f));
        float zFar = Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16F*2;
        mat.rotate(toRadians(90.0F), new Vector3f(0,1,0));
        mat.scale(new Vector3f(1/zFar, 1/zFar, 1/zFar));
        //Do hand rotations
        float f5 = player.prevRenderArmPitch + (player.renderArmPitch - player.prevRenderArmPitch) * partialTicks;
        float f6 = player.prevRenderArmYaw + (player.renderArmYaw - player.prevRenderArmYaw) * partialTicks;
        mat.rotate(toRadians((player.rotationPitch - f5) * 0.1F), new Vector3f(1, 0, 0));
        mat.rotate(toRadians((player.rotationYaw - f6) * 0.1F), new Vector3f(0, 1, 0));

        mat.rotate(toRadians(90), new Vector3f(0, 1, 0));
        mat.translate(new Vector3f(config.global.globalTranslate.x, config.global.globalTranslate.y, config.global.globalTranslate.z));
        mat.rotate(toRadians(-90), new Vector3f(0, 1, 0));
        mat.rotate(config.global.globalRotate.y/180*3.14f, new Vector3f(0, 1, 0));
        mat.rotate(config.global.globalRotate.x/180*3.14f, new Vector3f(1, 0, 0));
        mat.rotate(config.global.globalRotate.z/180*3.14f, new Vector3f(0, 0, 1));

        /**
         * ACTION GUN MOTION
         */
        float gunRotX = RenderParameters.GUN_ROT_X_LAST
                + (RenderParameters.GUN_ROT_X - RenderParameters.GUN_ROT_X_LAST) * ClientProxy.renderHooks.partialTicks;
        float gunRotY = RenderParameters.GUN_ROT_Y_LAST
                + (RenderParameters.GUN_ROT_Y - RenderParameters.GUN_ROT_Y_LAST) * ClientProxy.renderHooks.partialTicks;
        mat.rotate(toRadians(gunRotX), new Vector3f(0, -1, 0));
        mat.rotate(toRadians(gunRotY), new Vector3f(0, 0, -1));

        /**
         * ACTION GUN BALANCING X / Y
         */
        float rotateX=0;
        mat.translate(new Vector3f((float) (0.1f*GUN_BALANCING_X*Math.cos(Math.PI * RenderParameters.SMOOTH_SWING / 50)),0,0));
        rotateX-=(GUN_BALANCING_X * 4F) + (float) (GUN_BALANCING_X * Math.sin(Math.PI * RenderParameters.SMOOTH_SWING / 35));
        rotateX-=(float) Math.sin(Math.PI * GUN_BALANCING_X);
        rotateX-=(GUN_BALANCING_X) * 0.4F;
        mat.rotate(toRadians(rotateX),  new Vector3f(1f, 0f, 0f));

        floatBuffer.clear();
        mat.store(floatBuffer);
        floatBuffer.rewind();

        GL11.glMultMatrix(floatBuffer);


        model.updateAnimation(controller.getTime());


        /**
         * player right hand
         * */
        bindPlayerSkin();
        if(Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
            model.renderPart(RIGHT_SLIM_HAND_PART);
        }else {
            model.renderPart(RIGHT_HAND_PART);
        }

        /**
         * gun
         * */
        int skinId = 0;
        if (item.hasTagCompound()) {
            if (item.getTagCompound().hasKey("skinId")) {
                skinId = item.getTagCompound().getInteger("skinId");
            }
        }
        String meleePath = skinId > 0 ? meleeType.modelSkins[skinId].getSkin() : meleeType.modelSkins[0].getSkin();
        bindTexture("melee", meleePath);
        model.renderPart("meleeModel");

        /**
         * player left hand
         * */
        bindPlayerSkin();
        if(Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
            model.renderPart(LEFT_SLIM_HAND_PART);
        }else {
            model.renderPart(LEFT_HAND_PART);
        }

        GlStateManager.popMatrix();
    }

    @Override
    public void bindTexture(String type, String fileName) {
        super.bindTexture(type, fileName);
        String pathFormat = "skins/%s/%s.png";
        bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, String.format(pathFormat, type, fileName)));
    }

    public void bindTexture(ResourceLocation location) {
        bindingTexture = location;
    }

    public void bindPlayerSkin() {
        bindingTexture = Minecraft.getMinecraft().player.getLocationSkin();
    }

    public static float toRadians(float angdeg) {
        return angdeg / 180.0f * PI;
    }
}