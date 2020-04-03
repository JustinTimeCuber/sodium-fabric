package me.jellysquid.mods.sodium.mixin.entity_rendering;

import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.render.model.ExtendedCuboid;
import me.jellysquid.mods.sodium.client.render.model.FlattenedModelPartQuad;
import me.jellysquid.mods.sodium.client.render.pipeline.DirectVertexConsumer;
import me.jellysquid.mods.sodium.client.util.ColorUtil;
import me.jellysquid.mods.sodium.client.util.QuadUtil;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.*;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public class MixinModelPart {
    @Shadow
    @Final
    private ObjectList<ModelPart.Cuboid> cuboids;

    private final Vector4f posVec = new Vector4f();
    private final Vector3f normVec = new Vector3f();

    /**
     * @author JellySquid
     */
    @Overwrite
    private void renderCuboids(MatrixStack.Entry matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        if (vertexConsumer instanceof DirectVertexConsumer) {
            this.renderCuboidsDirect(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        } else {
            this.renderCuboidsFallback(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        }
    }

    private void renderCuboidsDirect(MatrixStack.Entry matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        Matrix4f modelMatrix = matrices.getModel();
        Matrix3f normalMatrix = matrices.getNormal();

        int color = ColorUtil.encodeRGBA(red, green, blue, alpha);

        final Vector3f normVec = this.normVec;
        final Vector4f posVec = this.posVec;

        for (ModelPart.Cuboid cuboid : this.cuboids) {
            for (ModelPart.Quad quad : ((ExtendedCuboid) cuboid).getQuads()) {
                Vector3f dir = quad.direction;
                normVec.set(dir.getX(), dir.getY(), dir.getZ());
                normVec.transform(normalMatrix);

                int norm = QuadUtil.encodeNormal(normVec);

                final float[] data = ((FlattenedModelPartQuad) quad).getFlattenedData();

                int k = 0;

                while (k < data.length) {
                    float x = data[k++];
                    float y = data[k++];
                    float z = data[k++];

                    float u = data[k++];
                    float v = data[k++];

                    posVec.set(x, y, z, 1.0f);
                    posVec.transform(modelMatrix);

                    ((DirectVertexConsumer) vertexConsumer).vertex(posVec.getX(), posVec.getY(), posVec.getZ(), color, u, v, overlay, light, norm);
                }
            }
        }
    }

    private void renderCuboidsFallback(MatrixStack.Entry matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        Matrix4f modelMatrix = matrices.getModel();
        Matrix3f normalMatrix = matrices.getNormal();

        final Vector3f normVec = this.normVec;
        final Vector4f posVec = this.posVec;

        for (ModelPart.Cuboid cuboid : this.cuboids) {
            for (ModelPart.Quad quad : ((ExtendedCuboid) cuboid).getQuads()) {
                Vector3f dir = quad.direction;
                normVec.set(dir.getX(), dir.getY(), dir.getZ());
                normVec.transform(normalMatrix);

                final float[] data = ((FlattenedModelPartQuad) quad).getFlattenedData();

                int k = 0;

                while (k < data.length) {
                    float x = data[k++];
                    float y = data[k++];
                    float z = data[k++];

                    float u = data[k++];
                    float v = data[k++];

                    posVec.set(x, y, z, 1.0f);
                    posVec.transform(modelMatrix);

                    vertexConsumer.vertex(posVec.getX(), posVec.getY(), posVec.getZ(), red, green, blue, alpha, u, v, overlay, light, normVec.getX(), normVec.getY(), normVec.getZ());
                }
            }
        }
    }

    @Mixin(ModelPart.Quad.class)
    private static class MixinQuad implements FlattenedModelPartQuad {
        private float[] data;

        @Inject(method = "<init>", at = @At("RETURN"))
        private void init(ModelPart.Vertex[] vertices, float float_1, float float_2, float float_3, float float_4, float float_5, float float_6, boolean boolean_1, Direction direction_1, CallbackInfo ci) {
            this.data = new float[vertices.length * 5];

            for (int i = 0; i < vertices.length; i++) {
                ModelPart.Vertex vertex = vertices[i];

                int j = i * 5;

                this.data[j] = vertex.pos.getX() / 16.0F;
                this.data[j + 1] = vertex.pos.getY() / 16.0F;
                this.data[j + 2] = vertex.pos.getZ() / 16.0F;

                this.data[j + 3] = vertex.u;
                this.data[j + 4] = vertex.v;
            }
        }

        @Override
        public float[] getFlattenedData() {
            return this.data;
        }
    }
}
