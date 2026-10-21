#version 330 compatibility

uniform sampler2D gtexture;
uniform sampler2D lightmap;
uniform float alphaTestRef = 0.1;
uniform vec4 entityColor;

in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 color;

void main() {
    color = texture(gtexture, texcoord) * glcolor;
    color.rgb = mix(color.rgb, entityColor.rgb, entityColor.a);
    color *= texture(lightmap, lmcoord);
    if (color.a < alphaTestRef) discard;
}
