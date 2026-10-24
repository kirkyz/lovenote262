package meteordevelopment.meteorclient.gui.screens;

import com.lovenote.Lovenote;
import com.lovenote.config.LovenoteConfig;
import com.lovenote.module.impl.SkyBlockCoach;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.ArrayList;

/** Standalone, client-side progression guide. Never automates gameplay. */
public final class SkyBlockGuideScreen extends Screen {
    private static final int W=850,H=470;
    private static final String[] TRACKS={"START","LEVELS","GARDEN","MINING","COMBAT","COINS"};
    private final Screen parent;
    private final List<int[]> hits=new ArrayList<>();
    private int scroll;
    private boolean gardenFocus;
    private float scale=1f;
    private int x,y;
    private String feedback="Progress is saved. Follow your in-game quest log for confirmation.";

    public SkyBlockGuideScreen(Screen parent){
        super(Component.literal("Drainer - SkyBlock Companion"));
        this.parent=parent;
    }
    private SkyBlockCoach coach(){
        return Lovenote.modules==null?null:Lovenote.modules.get(SkyBlockCoach.class);
    }
    @Override protected void init(){
        super.init();
        scale=Math.min(1f,Math.min((float)width/(W+16),(float)height/(H+16)));
        x=Math.max(0,((int)(width/scale)-W)/2);
        y=Math.max(0,((int)(height/scale)-H)/2);
    }
    private void rect(GuiGraphicsExtractor g,int xx,int yy,int ww,int hh,int color){
        if(ww>0&&hh>0)g.fill(x+xx,y+yy,x+xx+ww,y+yy+hh,color);
    }
    private void box(GuiGraphicsExtractor g,int xx,int yy,int ww,int hh,int bg,int border){
        rect(g,xx,yy,ww,hh,border);rect(g,xx+1,yy+1,ww-2,hh-2,bg);
    }
    private void txt(GuiGraphicsExtractor g,String s,int xx,int yy,int color){
        g.text(font,s,x+xx,y+yy,color,true);
    }
    private String trim(String value,int max){
        if(value==null)return "";
        if(font.width(value)<=max)return value;
        String s=value;
        while(s.length()>1&&font.width(s+"...")>max)s=s.substring(0,s.length()-1);
        return s+"...";
    }
    private void hit(int xx,int yy,int ww,int hh,int action){hits.add(new int[]{xx,yy,ww,hh,action});}
    private void button(GuiGraphicsExtractor g,int xx,int yy,int ww,String caption,int action,boolean selected){
        box(g,xx,yy,ww,28,selected?0xFF45235C:0xFF211B2F,selected?0xFFBE8CDF:0xFF47365D);
        txt(g,trim(caption,ww-15),xx+8,yy+9,selected?0xFFFFFFFF:0xFFE1D0EC);
        hit(xx,yy,ww,28,action);
    }
    private int wrap(GuiGraphicsExtractor g,String content,int xx,int yy,int maxWidth,int maxLines,int color){
        if(content==null)return yy;
        StringBuilder line=new StringBuilder();int count=0;
        for(String word:content.split("\\s+")){
            String test=line.isEmpty()?word:line+" "+word;
            if(font.width(test)>maxWidth&&!line.isEmpty()){
                txt(g,line.toString(),xx,yy,color);yy+=16;count++;line.setLength(0);
                if(count>=maxLines)return yy;
            }
            if(!line.isEmpty())line.append(' ');
            line.append(word);
        }
        if(!line.isEmpty()&&count<maxLines){txt(g,trim(line.toString(),maxWidth),xx,yy,color);yy+=16;}
        return yy;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        super.extractRenderState(g,mouseX,mouseY,delta);
        hits.clear();
        g.pose().pushMatrix();g.pose().scale(scale,scale);
        // Opaque near-black background keeps the standalone guide distinct from the module grid.
        rect(g,0,0,W,H,0xF6090812);
        rect(g,0,0,W,3,0xFFB574E7);
        txt(g,"DRAINER   /   SKYBLOCK COMPANION",24,18,0xFFF3DCFF);
        txt(g,"Your next action, where to go, and why it matters",24,38,0xFFAD9BBB);
        button(g,W-99,17,73,"BACK",-1,false);
        rect(g,0,61,W,1,0xFF3E2C52);
        SkyBlockCoach c=coach();
        if(c==null){txt(g,"SkyBlockCoach is unavailable. Check the module installation.",30,92,0xFFFF9F9F);g.pose().popMatrix();return;}
        var step=c.current();
        // LEFT: progression tracks
        box(g,12,74,157,376,0xFF12101D,0xFF47315D);
        txt(g,"PROGRESSION  "+c.completed()+" / "+c.total(),25,92,0xFFCBA7E9);
        for(int i=0;i<TRACKS.length;i++){
            boolean chosen=TRACKS[i].equals(c.track());
            button(g,23,119+i*43,135,TRACKS[i],i,chosen);
        }
        button(g,23,371,135,gardenFocus?"RETURN TO LIVE":"GARDEN PLAYBOOK",-10,gardenFocus);
        button(g,23,408,135,c.benchmarkRemaining()>=0?"RESET TEST":"START 5M TEST",-11,false);
        // CENTER: big readable current objective
        box(g,181,74,424,376,0xFF151121,0xFF614477);
        txt(g,"CURRENT OBJECTIVE   /   "+c.track(),198,89,0xFFCE9CEB);
        rect(g,198,109,388,4,0xFF352A42);
        int pw=(int)(388*c.completedTrack()/(float)Math.max(1,c.trackCount()));
        rect(g,198,109,pw,4,0xFFD18AFF);
        int ty=133;
        ty=wrap(g,step.title(),198,ty,377,2,0xFFFFFFFF)+8;
        txt(g,"01  WHERE TO GO",198,ty,0xFFE5A8FF);ty+=17;
        ty=wrap(g,SkyBlockCoach.destination(step),198,ty,377,2,0xFFEAE0F3)+6;
        txt(g,"02  WHAT TO DO",198,ty,0xFFE5A8FF);ty+=17;
        ty=wrap(g,step.action(),198,ty,377,3,0xFFEEE9F6)+6;
        txt(g,"03  WHY THIS HELPS",198,ty,0xFFE5A8FF);ty+=17;
        wrap(g,step.reason(),198,ty,377,3,0xFFCEC1DD);
        String roi=c.coinsPerHour()>0
            ?"BEST TESTED: "+c.bestMethod()+" / "+String.format(java.util.Locale.ROOT,"%,.0f",c.coinsPerHour())+" coins/h   PAYBACK: "+c.payback()
            :"COIN PLANNER: enter your 5-minute samples in module settings";
        txt(g,trim(roi,388),198,366,0xFFC69DDD);
        if(c.track().equals("START") && c.current().title().equals("Unlock your first skills") && !c.currentDone())
            button(g,197,380,213,"ALREADY UNLOCKED SKILLS",-14,false);
        button(g,197,412,93,"PREVIOUS",-2,false);
        button(g,298,412,153,c.currentDone()?"MARK UNDONE":"MARK COMPLETE",-3,true);
        button(g,459,412,128,"NEXT STEP",-4,false);
        // RIGHT: live connection, quest evidence, GPS and recommendation controls.
        box(g,615,74,223,376,0xFF13101E,0xFF523765);
        var live=c.live();
        if(gardenFocus){
            txt(g,"GARDEN / MANUAL COACH",628,92,0xFFDDB7F5);
            txt(g,"SELECTED CROP",628,115,0xFFB880DA);
            button(g,627,132,196,c.gardenCrop()+"  >",-12,true);
            txt(g,"FARM DESIGN",628,180,0xFFB880DA);
            wrap(g,c.gardenLayout(),628,196,187,4,0xFFE7D7F1);
            txt(g,"UPGRADE ORDER",628,262,0xFFB880DA);
            wrap(g,c.gardenUpgrade(),628,277,188,3,0xFFDED5F3);
            txt(g,"5-MINUTE SELL TEST",628,338,0xFFB880DA);
            wrap(g,c.gardenSale(),628,352,190,3,0xFFE6D9EB);
            txt(g,trim(c.benchmarkLabel(),191),628,421,0xFFF5C884);
        } else {
            txt(g,"LIVE SKYBLOCK ASSISTANT",628,92,0xFFDDB7F5);
            txt(g,"ISLAND / LEVEL",628,116,0xFFB880DA);
            String area=live.hypixel()?live.area()+ (live.level()>=0?"  •  LV "+live.level():""):"Offline preview";
            txt(g,trim(area,190),628,132,0xFFEDE5F4);
            txt(g,"VISIBLE QUEST (NOT VERIFIED)",628,152,0xFFB880DA);
            wrap(g,live.quest(),628,168,190,2,0xFFE3D5F1);
            txt(g,"PROFILE / ECONOMY",628,208,0xFFB880DA);
            txt(g,trim(live.profile()+" / "+live.apiStatus(),190),628,226,0xFFECE1F7);
            txt(g,trim(live.market(),190),628,244,0xFFBDA9CF);
            txt(g,"WAYPOINT / DIRECTION",628,268,0xFFB880DA);
            wrap(g,c.waypointText(),628,284,190,2,0xFFDED5F3);
            button(g,626,326,67,"GO TO",-6,false);
            button(g,698,326,61,"PIN HERE",-7,false);
            button(g,765,326,61,"CLEAR",-8,false);
            button(g,627,361,91,c.hudVisible()?"HUD ON":"HUD OFF",-5,c.hudVisible());
            button(g,722,361,101,"HUD: "+c.hudLayout(),-13,false);
            button(g,627,401,196,"SUGGEST MY NEXT STEP",-9,true);
        }
        txt(g,trim(feedback,810),24,455,0xFF9D8DB2);
        g.pose().popMatrix();
    }
    private void save(){LovenoteConfig.save(true);}
    @Override public boolean mouseClicked(MouseButtonEvent click,boolean doubled){
        int mx=(int)(click.x()/scale)-x,my=(int)(click.y()/scale)-y;
        for(int[] h:hits){
            if(mx<h[0]||my<h[1]||mx>=h[0]+h[2]||my>=h[1]+h[3])continue;
            SkyBlockCoach c=coach();int action=h[4];
            if(action==-1){onClose();return true;}
            if(c==null)return true;
            if(action>=0&&action<TRACKS.length){c.setTrack(TRACKS[action]);feedback="Track selected. Confirm completion manually.";}
            if(action==-2)c.advance(-1);
            if(action==-3){c.mark(!c.currentDone());feedback="Saved. Review the in-game quest log before marking done.";}
            if(action==-4)c.advance(1);
            if(action==-5)c.toggleHud();
            if(action==-6)feedback=c.pinSuggested()?"Pinned a verified location for this goal.":"No verified coordinates. Use PIN HERE for a custom marker.";
            if(action==-7)feedback=c.pinHere()?"Saved your current location as a custom waypoint.":"Join a world before pinning a location.";
            if(action==-8){c.clearPin();feedback="Custom waypoint cleared.";}
            if(action==-9)feedback=c.suggestNext();
            if(action==-10){gardenFocus=!gardenFocus;feedback=gardenFocus?"Choose a crop to see the manual Garden plan.":"Live quest view restored.";}
            if(action==-11){c.toggleBenchmark();feedback=c.benchmarkRemaining()>=0?"Manual 5-minute test started. Record real net proceeds.":"Test cleared.";}
            if(action==-12){c.nextGardenCrop();feedback="Crop changed; check the layout, fortune and real sale values.";}
            if(action==-13){c.cycleHudLayout();feedback="HUD layout: "+c.hudLayout()+". You can also adjust HUD Width in module settings.";}
            if(action==-14){c.confirmSkillsUnlocked();feedback="Marked skills unlocked on your confirmation. Other steps still need evidence.";}
            save();return true;
        }
        return true;
    }
    @Override public boolean keyPressed(KeyEvent input){
        if(input.key()==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}
        return super.keyPressed(input);
    }
    @Override public void onClose(){
        save();minecraft.gui.setScreen(parent);
    }
    @Override public boolean isPauseScreen(){return false;}
}
