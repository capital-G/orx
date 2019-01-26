#version 330 core

uniform sampler2D tex0;
in vec2 v_texCoord0;
out vec4 o_color;

uniform int passIndex;
uniform int sampleCount;
uniform int sampleCountBase;
uniform vec2 passDirection;


void main() {
    vec2 passOffset = vec2(pow(sampleCountBase, passIndex)) * (1.0/textureSize(tex0, 0)) * passDirection;

    vec2 uv0 = v_texCoord0;
    vec4 result = vec4(0.0);
    for (int i = 0; i < sampleCount; ++i) {
        vec2 readUV = v_texCoord0 - vec2(i *passOffset);
        float factor = step(0.0, readUV.x) * step(0.0, readUV.y);
        result += factor * texture(tex0, readUV);
    }

    o_color = result;

}