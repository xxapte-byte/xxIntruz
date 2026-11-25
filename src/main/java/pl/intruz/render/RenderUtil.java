package pl.intruz.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public final class RenderUtil {
    private RenderUtil() {}

    /** macierz widoku (taka sama jak w dzialajacych tracerach PMod) */
    public static Matrix4f view(WorldRenderContext c) {
        MatrixStack ms = c.matrixStack();
        return ms != null ? ms.peek().getPositionMatrix() : c.positionMatrix();
    }

    public static BufferBuilder begin(boolean depth, float width) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        if (!depth) RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(width);
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
    }

    /** koniec rysowania - zawsze konczy builder (nawet pusty), zeby nie zostal "wiszacy" */
    public static void end(BufferBuilder b) {
        BuiltBuffer built = b.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(1f);
    }

    /** linia we wspolrzednych wzglednych do kamery */
    public static void line(BufferBuilder b, Matrix4f m, float x1, float y1, float z1,
                            float x2, float y2, float z2, int argb) {
        b.vertex(m, x1, y1, z1).color(argb);
        b.vertex(m, x2, y2, z2).color(argb);
    }
}
