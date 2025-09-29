package com.akademiaplus.util;

import java.util.List;

public interface DataFactory <D> {

    public List<D> generate(int count);
}
