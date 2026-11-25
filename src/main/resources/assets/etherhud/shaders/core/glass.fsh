#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 localPosition;
flat in vec2 halfSize;
flat in vec4 radii;
flat in vec4 fillColor;
flat in vec4 borderColor;
flat in float borderWidth;

out vec4 fragColor;

void main() {
    vec2 p = localPosition;
    float radius = p.y < 0.0
        ? (p.x < 0.0 ? radii.x : radii.y)
        : (p.x < 0.0 ? radii.z : radii.w);
    vec2 q = abs(p) - halfSize + radius;
    float distanceToEdge = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
    float aa = max(0.5 * fwidth(distanceToEdge), 0.001);
    float outer = 1.0 - smoothstep(-aa, aa, distanceToEdge);
    float inner = borderWidth == 0.0 ? outer : 1.0 - smoothstep(-aa, aa, distanceToEdge + borderWidth);
    float fillAlpha = fillColor.a * inner;
    float borderAlpha = borderColor.a * (outer - inner);
    float alpha = fillAlpha + borderAlpha;
    if (alpha <= 0.0) {
        discard;
    }
    vec4 color = vec4((fillColor.rgb * fillAlpha + borderColor.rgb * borderAlpha) / alpha, alpha);
    fragColor = color * ColorModulator;
}
