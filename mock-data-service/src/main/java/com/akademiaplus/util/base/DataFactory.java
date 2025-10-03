package com.akademiaplus.util.base;

import java.util.List;

public interface DataFactory <D> {

    List<D> generate(int count);
}
