package com.echo.adaper;
/*
 * ²ÉÓÃ¼Ì³Ğ·½Ê½µÄ²å×ùÊÊÅäÆ÷
 */

public class TwoPlugAdapterExtends extends GBTwoPlug implements ThreePlugIf {

	@Override
	public void powerWithThree() {
		// TODO Auto-generated method stub
		System.out.print("½èÖú¼Ì³ĞÊÊÅäÆ÷");
		this.powerWithTwo();
	}

}
