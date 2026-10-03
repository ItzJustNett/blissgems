#version 330 compatibility

// BlissGems — GloomHaze + GoldenDome в одному паку.
// Звичайний світ лишається звичайним. Ефект вмикає сервер (плагін BlissGems):
// він ставить гравцю «сигнальний» день (worldDay), і пак сам обирає вигляд:
//   день 80036 -> GoldenDome (золота стежка сну, ритуал і золота маса)
//   день 80037 -> GloomHaze (світ пам'яті)
//   день 80038 -> GloomHaze під час бурі (великого грому)
// FORCE_MODE: 0 = слухати сервер, 1 = завжди GoldenDome, 2 = завжди GloomHaze (для тестів у соло).

#define FORCE_MODE 0           // [0 1 2]

// ---- GloomHaze — похмурий, знебарвлений, туманний вигляд світу ----
#define GLOOM_SATURATION 0.35  // [0.0 0.1 0.2 0.3 0.35 0.4 0.5 0.6 0.8 1.0]
#define FOG_DENSITY 1.0        // [0.0 0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define FOG_START 0.0          // [0.0 0.1 0.2 0.3]
#define BRIGHTNESS 0.85        // [0.6 0.7 0.8 0.85 0.9 1.0 1.1 1.2]
#define CONTRAST 0.85          // [0.6 0.7 0.8 0.85 0.9 1.0 1.1]
#define WARM_SUNSET 1.0        // [0.0 0.5 1.0 1.5]
#define VIGNETTE 0.45          // [0.0 0.2 0.3 0.45 0.6 0.8]
#define GRAIN 0.035            // [0.0 0.015 0.025 0.035 0.05 0.08]
#define BLACK_LIFT 0.03        // [0.0 0.015 0.03 0.05]

// ---- GoldenDome — золотий сотовий купол навколо гравця ----
#define DOME_RADIUS 48.0       // [16.0 24.0 32.0 48.0 64.0 96.0 128.0 256.0 1000.0]
#define CELL_DENSITY 7.0       // [3.0 4.0 5.0 6.0 7.0 9.0 12.0 16.0]
#define DOME_OPACITY 0.95      // [0.3 0.5 0.7 0.85 0.95 1.0]
#define EDGE_GLOW 1.0          // [0.5 0.75 1.0 1.5 2.0]
#define ANIM_SPEED 0.25        // [0.0 0.1 0.25 0.5 1.0]
#define WORLD_WARMTH 0.35      // [0.0 0.15 0.25 0.35 0.5 0.75]
#define DOME_SATURATION 1.15   // [0.8 1.0 1.15 1.3 1.5]
#define SPARKLES 1.0           // [0.0 0.5 1.0 2.0]

#define SIGNAL_DOME 80036
#define SIGNAL_GLOOM 80037
#define SIGNAL_STORM 80038

uniform sampler2D colortex0;
uniform sampler2D depthtex0;

uniform mat4 gbufferProjectionInverse;
uniform mat4 gbufferModelViewInverse;
uniform float far;
uniform float viewWidth;
uniform float viewHeight;
uniform float frameTimeCounter;
uniform float sunAngle;
uniform float rainStrength;
uniform int isEyeInWater;
uniform ivec2 eyeBrightnessSmooth;
uniform int worldDay;

in vec2 texcoord;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

vec3 hash33(vec3 p) {
    p = vec3(dot(p, vec3(127.1, 311.7, 74.7)),
             dot(p, vec3(269.5, 183.3, 246.1)),
             dot(p, vec3(113.5, 271.9, 124.6)));
    return fract(sin(p) * 43758.5453123);
}

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

// ================= GloomHaze =================
vec3 gloomHaze(vec3 color, float depth, float dist) {
    bool isSky = depth >= 1.0;

    // час доби: sunAngle 0..0.5 день, 0.5..1 ніч
    float sunHeight = sin(sunAngle * 6.2831853);
    float day = smoothstep(-0.1, 0.25, sunHeight);
    float sunset = (1.0 - smoothstep(0.0, 0.35, abs(sunHeight))) * WARM_SUNSET;

    // колір туману: сірий холодний вночі, сіро-світлий вдень, бурштиновий на заході
    vec3 nightFog  = vec3(0.045, 0.05, 0.06);
    vec3 dayFog    = vec3(0.42, 0.42, 0.40);
    vec3 sunsetFog = vec3(0.55, 0.38, 0.24);
    vec3 fogColor = mix(nightFog, dayFog, day);
    fogColor = mix(fogColor, sunsetFog, clamp(sunset, 0.0, 1.0) * 0.8);
    fogColor = mix(fogColor, fogColor * 0.6, rainStrength);

    // у печерах туман темніший
    float skyLight = eyeBrightnessSmooth.y / 240.0;
    fogColor *= mix(0.25, 1.0, skyLight);

    // туман за відстанню
    float d = max(dist / far - FOG_START, 0.0);
    float density = FOG_DENSITY * (2.2 + rainStrength * 2.0);
    float fog = 1.0 - exp(-d * density * d * 3.0 - d * density * 0.6);
    if (isSky) fog = 0.5;
    if (isEyeInWater == 1) fog = 1.0 - exp(-dist * 0.08);
    color = mix(color, fogColor, clamp(fog, 0.0, 1.0));

    // знебарвлення
    float l = luma(color);
    color = mix(vec3(l), color, GLOOM_SATURATION);

    // легкий теплий/холодний тон
    vec3 tint = mix(vec3(0.92, 0.96, 1.02), vec3(1.05, 0.98, 0.88), max(day * 0.4, sunset));
    color *= tint;

    // яскравість, контраст, підняті чорні («матовий» вигляд)
    color *= BRIGHTNESS;
    color = (color - 0.5) * CONTRAST + 0.5;
    color = max(color, 0.0);
    color = color * (1.0 - BLACK_LIFT) + BLACK_LIFT;

    // віньєтка
    vec2 uv = texcoord - 0.5;
    uv.x *= viewWidth / viewHeight;
    float vig = 1.0 - smoothstep(0.35, 1.05, length(uv)) * VIGNETTE;
    color *= vig;

    // плівкове зерно
    float n = hash(texcoord * vec2(viewWidth, viewHeight) + fract(frameTimeCounter) * 100.0) - 0.5;
    color += n * GRAIN;
    return color;
}

// ================= GoldenDome =================
// повертає (відстань до найближчої клітинки, відстань до межі, id клітинки)
vec3 voronoi(vec3 p, float t) {
    vec3 ip = floor(p);
    vec3 fp = fract(p);

    float f1 = 8.0;
    vec3 mr = vec3(0.0);
    vec3 mg = vec3(0.0);
    for (int z = -1; z <= 1; z++)
    for (int y = -1; y <= 1; y++)
    for (int x = -1; x <= 1; x++) {
        vec3 g = vec3(x, y, z);
        vec3 o = hash33(ip + g);
        o = 0.5 + 0.4 * sin(t + 6.2831 * o);
        vec3 r = g + o - fp;
        float d = dot(r, r);
        if (d < f1) { f1 = d; mr = r; mg = g; }
    }

    // точна відстань до межі клітинки
    float edge = 8.0;
    for (int z = -2; z <= 2; z++)
    for (int y = -2; y <= 2; y++)
    for (int x = -2; x <= 2; x++) {
        vec3 g = mg + vec3(x, y, z);
        vec3 o = hash33(ip + g);
        o = 0.5 + 0.4 * sin(t + 6.2831 * o);
        vec3 r = g + o - fp;
        vec3 diff = r - mr;
        if (dot(diff, diff) > 0.00001)
            edge = min(edge, dot(0.5 * (mr + r), normalize(diff)));
    }

    return vec3(sqrt(f1), edge, hash13(ip + mg));
}

vec3 domeColor(vec3 dir) {
    float t = frameTimeCounter * ANIM_SPEED;
    vec3 v = voronoi(dir * CELL_DENSITY, t);
    float center = v.x;
    float edge = v.y;
    float id = v.z;

    // тіло клітинки: насичене золото, світліше до країв
    vec3 goldDark  = vec3(0.55, 0.32, 0.05);
    vec3 goldLight = vec3(0.95, 0.68, 0.22);
    vec3 cell = mix(goldDark, goldLight, 0.35 + 0.45 * id);
    cell *= 0.75 + 0.6 * smoothstep(0.0, 0.6, center);

    // дрібні іскорки всередині клітинок
    vec3 sp = floor(dir * 220.0);
    float spark = step(0.985, hash13(sp + floor(t * 3.0)));
    cell += spark * vec3(1.0, 0.9, 0.6) * 0.6 * SPARKLES;

    // білі сяючі грані
    float line = 1.0 - smoothstep(0.02, 0.06, edge);
    float glow = exp(-edge * 14.0);
    vec3 col = cell;
    col = mix(col, vec3(1.0, 0.97, 0.85), line);
    col += vec3(1.0, 0.8, 0.35) * glow * 0.6 * EDGE_GLOW;
    return col;
}

vec3 goldenDome(vec3 color, float depth, float viewDist) {
    float dist = depth >= 1.0 ? 1e6 : viewDist;

    // напрямок погляду у світових координатах
    vec4 farPos = gbufferProjectionInverse * vec4(texcoord * 2.0 - 1.0, 1.0, 1.0);
    vec3 viewDir = normalize(farPos.xyz / farPos.w);
    vec3 worldDir = normalize(mat3(gbufferModelViewInverse) * viewDir);

    // теплий золотистий тон світу
    float l = luma(color);
    color = mix(vec3(l), color, DOME_SATURATION);
    color *= mix(vec3(1.0), vec3(1.12, 1.0, 0.78), WORLD_WARMTH);

    // купол: усе, що далі за радіус, закрите куполом
    float behindDome = smoothstep(DOME_RADIUS - 2.0, DOME_RADIUS + 2.0, dist);
    if (behindDome > 0.0) {
        color = mix(color, domeColor(worldDir), behindDome * DOME_OPACITY);
    }

    // золотисте світло від купола на світі поблизу краю
    float nearDome = smoothstep(DOME_RADIUS * 0.4, DOME_RADIUS, dist) * (1.0 - behindDome);
    color += vec3(0.25, 0.16, 0.04) * nearDome * EDGE_GLOW;

    // м'який тонмапінг, щоб яскраві грані не «вигорали» різко
    return color / (1.0 + max(color - 1.0, 0.0));
}

void main() {
    vec3 color = texture(colortex0, texcoord).rgb;

    int mode = FORCE_MODE;
    if (mode == 0) {
        if (worldDay == SIGNAL_DOME) mode = 1;
        else if (worldDay == SIGNAL_GLOOM || worldDay == SIGNAL_STORM) mode = 2;
    }
    if (mode == 0) {
        // звичайний світ — без змін
        outColor = vec4(color, 1.0);
        return;
    }

    float depth = texture(depthtex0, texcoord).r;
    vec4 ndc = vec4(texcoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPos = gbufferProjectionInverse * ndc;
    viewPos.xyz /= viewPos.w;
    float dist = length(viewPos.xyz);

    color = mode == 1 ? goldenDome(color, depth, dist) : gloomHaze(color, depth, dist);
    outColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
