package com.resimulators.simukraft.client.gui;

import com.resimulators.simukraft.common.tileentity.TileMiner;
import com.resimulators.simukraft.network.ClientUpdateMIningPacket;
import com.resimulators.simukraft.network.MinerUpdateDataPacket;
import com.resimulators.simukraft.network.PacketHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;

public class GuiMiner extends GuiScreen {
    TileMiner miner;
    private int x = 0;
    private String status = "";
    private GuiButton button1;
    private GuiButton button2;
    private GuiButton applybutton;
    private GuiButton minebutton;
    private int mouseX;
    private int mouseY;
    private int mode;
    private int widths;
    private int heights;
    private int depths;
    private GuiTextField heightbox;
    private GuiTextField depthbox;
    private GuiTextField widthbox;


    public GuiMiner(TileMiner miner){
        this.miner = miner;
        mode = miner.getMode();
        widths = miner.getWidth();
        heights = miner.getHeight();
        depths = miner.getDepth();
    }
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (mode == 0){
            mc.fontRenderer.drawString("Quarry Mode",width/4-mc.fontRenderer.getStringWidth("Quarry Mode")/2,height - 100,Color.WHITE.getRGB());
            depthbox.drawTextBox();
            mc.fontRenderer.drawString("Depth",width/4*3+50,height-100,Color.WHITE.getRGB());
        }
        if (mode == 1){
            mc.fontRenderer.drawString("StripMine Mode",width/4-mc.fontRenderer.getStringWidth("StripMine Mode")/2,height - 100,Color.WHITE.getRGB());
            mc.fontRenderer.drawString("Height",width/4*3+50,height-100,Color.WHITE.getRGB());
            heightbox.drawTextBox();
        }
        widthbox.drawTextBox();
        mc.fontRenderer.drawString("Width",width/4*3,height-100,Color.WHITE.getRGB());
        mc.fontRenderer.drawString("Render Outline",width/4-100,height-50,Color.WHITE.getRGB());
        mc.fontRenderer.drawString("Mining",width/4*3 -100,height-100,Color.WHITE.getRGB());
        super.drawScreen(mouseX, mouseY, partialTicks);
    }


    @Override
    public void updateScreen(){
        super.updateScreen();
        heightbox.updateCursorCounter();
        depthbox.updateCursorCounter();
        widthbox.updateCursorCounter();
    }

    @Override
    public void initGui() {
        buttonList.add(button1 = new GuiButton(0,width/4-100,height - 80,"Change Mode"));
        buttonList.add(button2 = new GuiButton( 1,width/4-100,height-30,String.valueOf(miner.isRenderOutline())));
        buttonList.add(applybutton = new GuiButton(2,width/4*3-100,height-30,"Apply"));
        buttonList.add(minebutton = new GuiButton(3,width/4 *3 - 100,height - 80,80,20,Boolean.toString(miner.isShouldmine())));
        heightbox = new GuiTextField(1,mc.fontRenderer,width/4*3+50,height-80,30,20);
        heightbox.setMaxStringLength(3);
        heightbox.setText(String.valueOf(miner.getHeight()));
        depthbox = new GuiTextField(2,mc.fontRenderer,width/4*3+50,height-80,30,20);
        depthbox.setMaxStringLength(3);
        depthbox.setText(String.valueOf(miner.getDepth()));
        widthbox = new GuiTextField(2,mc.fontRenderer,width/4*3,height-80,30,20);
        widthbox.setMaxStringLength(3);
        widthbox.setText(String.valueOf(miner.getWidth()));

        super.initGui();
    }


    @Override
    protected void mouseClicked(int x, int y, int btn){
        heightbox.mouseClicked(x,y,btn);
        if (heightbox.mouseClicked(x,y,btn)) heightbox.setText("");
        depthbox.mouseClicked(x,y,btn);
        if (depthbox.mouseClicked(x,y,btn)) heightbox.setText("");
        widthbox.mouseClicked(x,y,btn);
        if (widthbox.mouseClicked(x,y,btn)) heightbox.setText("");
        try {
            super.mouseClicked(x,y,btn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
       if (button.id == 0){
                if (mode == 0){ mode = 1;
                }
                if (mode == 1) mode = 0;
                }
            else if (button.id == 1){
                miner.setRenderOutline(!miner.isRenderOutline());
                button.displayString = String.valueOf(miner.isRenderOutline());
                }
                else if (button.id == 2){
                applychanges();
                }
                else if(button.id == 3){
                    if (miner.isShouldmine()){
                        miner.setShouldmine(false);
                        button.displayString = "False";
                    }
                    else {
                        miner.setShouldmine(true);
                        button.displayString = "True";
                    }
                    PacketHandler.INSTANCE.sendToServer(new ClientUpdateMIningPacket(miner.isShouldmine(),miner.getPos()));

       }

        super.actionPerformed(button);
    }


    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }




    @Override
    protected void keyTyped(char typedchar,int keycode) throws IOException {
        if (Character.isDigit(typedchar) || keycode == Keyboard.KEY_BACK){
        if (heightbox.isFocused())heightbox.textboxKeyTyped(typedchar,keycode);
        if (depthbox.isFocused())depthbox.textboxKeyTyped(typedchar,keycode);
        if (widthbox.isFocused())widthbox.textboxKeyTyped(typedchar, keycode);
        }else{
            super.keyTyped(typedchar,keycode);
        }
    }


    @Override
    public void onGuiClosed(){

    }

    private void applychanges(){
        if (heightbox.getText().equals(""))heights = 0;
        else heights = Integer.parseInt(heightbox.getText());
        if (depthbox.getText().equals("")) heights = 0;
        else depths = Integer.parseInt(depthbox.getText());
        widths = Integer.parseInt(widthbox.getText());
        miner.setHeight(heights);
        miner.setWidth(widths);
        miner.setDepth(depths);

        miner.setMode(mode);
        PacketHandler.INSTANCE.sendToServer(new MinerUpdateDataPacket(miner.serializeNBT(),miner.getPos()));
    }
}