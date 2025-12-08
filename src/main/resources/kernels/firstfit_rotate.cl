
__kernel void firstfit_rotate(
    float box_w, float box_h, float box_d,
    __global const float *spaces,
    __global float *results
) {
    int gid = get_global_id(0);
    // Assuming Space struct in memory is float[3]: {w, h, d}
    int space_offset = gid * 3;
    float space_w = spaces[space_offset + 0];
    float space_h = spaces[space_offset + 1];
    float space_d = spaces[space_offset + 2];

    // Result is 4 elements per space [fits, w, h, d] (box dimensions)
    int res_offset = gid * 4;
    
    // Default to not fitting
    results[res_offset] = 0.0f; 
    results[res_offset + 1] = 0.0f;
    results[res_offset + 2] = 0.0f;
    results[res_offset + 3] = 0.0f;

    // 1. (x, y, z)
    if (box_w <= space_w && box_h <= space_h && box_d <= space_d) {
        results[res_offset] = 1.0f;
        results[res_offset + 1] = box_w;
        results[res_offset + 2] = box_h;
        results[res_offset + 3] = box_d;
        return;
    }

    // 2. (x, z, y) -> (w, d, h)
    if (box_w <= space_w && box_d <= space_h && box_h <= space_d) {
        results[res_offset] = 1.0f;
        results[res_offset + 1] = box_w;
        results[res_offset + 2] = box_d;
        results[res_offset + 3] = box_h;
        return;
    }

    // 3. (y, x, z) -> (h, w, d)
    if (box_h <= space_w && box_w <= space_h && box_d <= space_d) {
        results[res_offset] = 1.0f;
        results[res_offset + 1] = box_h;
        results[res_offset + 2] = box_w;
        results[res_offset + 3] = box_d;
        return;
    }

    // 4. (y, z, x) -> (h, d, w)
    if (box_h <= space_w && box_d <= space_h && box_w <= space_d) {
        results[res_offset] = 1.0f;
        results[res_offset + 1] = box_h;
        results[res_offset + 2] = box_d;
        results[res_offset + 3] = box_w;
        return;
    }

    // 5. (z, x, y) -> (d, w, h)
    if (box_d <= space_w && box_w <= space_h && box_h <= space_d) {
        results[res_offset] = 1.0f;
        results[res_offset + 1] = box_d;
        results[res_offset + 2] = box_w;
        results[res_offset + 3] = box_h;
        return;
    }

    // 6. (z, y, x) -> (d, h, w)
    if (box_d <= space_w && box_h <= space_h && box_w <= space_d) {
        results[res_offset] = 1.0f;
        results[res_offset + 1] = box_d;
        results[res_offset + 2] = box_h;
        results[res_offset + 3] = box_w;
        return;
    }
}

