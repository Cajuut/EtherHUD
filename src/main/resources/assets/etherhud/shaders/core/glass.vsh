#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec2 localPosition;
flat out vec2 halfSize;
flat out vec4 radii;
flat out vec4 fillColor;
flat out vec4 borderColor;
flat out float borderWidth;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    localPosition = UV0;
    halfSize = abs(UV0) - vec2(2.0);
    radii = vec4(UV1.x & 255, (UV1.x >> 8) & 255, UV1.y & 255, (UV1.y >> 8) & 255) * 0.25;
    fillColor = Color;
    borderColor = vec4((UV2.x >> 8) & 255, UV2.x & 255, (UV2.y >> 8) & 255, UV2.y & 255) / 255.0;
    borderWidth = round(Normal.x * 127.0);
}
