// Template paints/text use dedicated corner stamps. RGB is consumer color, not a layout opcode.
bool duiWorldHud() {
    if (ProjMat[2][3] != 0.0) return false;
    ivec2 atlasSize = textureSize(Sampler0, 0);
    ivec4 stamp = ivec4(round(texelFetch(Sampler0, ivec2(UV0 * vec2(atlasSize)), 0) * 255.0));
    if ((stamp.r != DUI_MAP_HUD_STAMP_COLOR && stamp.r != DUI_MAP_HUD_MASK_STAMP_COLOR) || stamp.a < 128 || stamp.a > 131) return false;
    vec2 corner = vec2((stamp.a - 128) & 1, (stamp.a - 128) >> 1);
    vec2 ui = vec2(2.0 / ProjMat[0][0], -2.0 / ProjMat[1][1]);
    vec2 p;
    vertexColor = Color;
    if (stamp.r == DUI_MAP_HUD_MASK_STAMP_COLOR) {
        if(stamp.g==1) p=vec2(ui.x*0.5-91.0+corner.x*182.0,12.0+corner.y*5.0);
        else p = vec2(ui.x * 0.5 - 104.0 + corner.x * 208.0, ui.y - 47.0 + corner.y * 51.0);
    } else {
        vec2 size = vec2(stamp.gb);
        vec2 cropped = Position.xy + (1.0 - 2.0 * corner);
        vec2 origin = cropped - corner * size - vec2(floor(ui.x * 0.5), 3.0);
        int code = int(floor(origin.x / float(DUI_MAP_HUD_PEN_STRIDE)));
        if (code < 0 || code >= 4608) return false;
        int opacity = code & 255;
        int mode = code >> 8;
        int anchor = mode >> 1;
        float signY = (mode & 1) == 0 ? 1.0 : -1.0;
        vec2 local = vec2(origin.x - float(code * DUI_MAP_HUD_PEN_STRIDE) - float(DUI_MAP_HUD_PEN_BIAS), signY * origin.y);
        float physical = max(1.0, floor(min(ScreenSize.x / float(DUI_MAP_HUD_VIRTUAL_WIDTH), ScreenSize.y / float(DUI_MAP_HUD_VIRTUAL_HEIGHT))));
        float scale = physical * ui.y / ScreenSize.y;
        p = vec2(float(anchor % 3), float(anchor / 3)) * ui * 0.5 + (local + corner * size) * scale;
        vertexColor.a *= float(opacity) / 255.0;
    }
    gl_Position = vec4(p / ui * vec2(2.0, -2.0) + vec2(-1.0, 1.0), stamp.r == DUI_MAP_HUD_MASK_STAMP_COLOR ? 0.0 : 0.9999, 1.0);
    texCoord0 = UV0 + (1.0 - 2.0 * corner) / vec2(atlasSize);
    return true;
}
