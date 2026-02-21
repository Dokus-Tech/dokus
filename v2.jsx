import { useState, useEffect, useRef } from "react";

const fl = document.createElement("link");
fl.href = "https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;600;700&display=swap";
fl.rel = "stylesheet";
document.head.appendChild(fl);

const ui = `'Inter', -apple-system, sans-serif`;
const num = `'JetBrains Mono', monospace`;

const C = {
  bg: "#f2f1ee", page: "#ffffff", canvas: "#f0efec", warm: "#f8f7f4",
  glass: "rgba(255,255,255,0.42)", glassContent: "rgba(255,255,255,0.58)",
  glassBorder: "rgba(255,255,255,0.5)", glassHeader: "rgba(255,255,255,0.35)",
  text: "#1a1815", textSec: "#5c5650", textMuted: "#9c958c", textFaint: "#d4d0ca", thText: "#4a443d",
  border: "rgba(0,0,0,0.06)", borderStrong: "rgba(0,0,0,0.10)", borderAmber: "rgba(180,100,10,0.18)",
  amber: "#b8860b", amberMed: "#d4a017", amberSoft: "rgba(184,134,11,0.07)", amberWhisper: "rgba(184,134,11,0.03)",
  red: "#c0392b", redSoft: "rgba(192,57,43,0.06)", green: "#1e8449", greenSoft: "rgba(30,132,73,0.06)",
  shadow: "0 1px 3px rgba(0,0,0,0.04)",
};

const CSS = `
  @keyframes rise { from { opacity:0; transform:translateY(5px); } to { opacity:1; transform:translateY(0); } }
  @keyframes bar-grow { from { transform:scaleY(0); } to { transform:scaleY(1); } }
  @keyframes pulse-dot { 0%,100% { box-shadow:0 0 0 0 rgba(30,132,73,0.3); } 50% { box-shadow:0 0 0 4px rgba(30,132,73,0); } }
  @keyframes drift-1 { 0%,100%{transform:translate(-50%,-50%) translate(0,0) scale(1);} 25%{transform:translate(-50%,-50%) translate(80px,-50px) scale(1.15);} 50%{transform:translate(-50%,-50%) translate(-40px,60px) scale(0.95);} 75%{transform:translate(-50%,-50%) translate(50px,25px) scale(1.08);} }
  @keyframes drift-2 { 0%,100%{transform:translate(-50%,-50%) translate(0,0) scale(1);} 25%{transform:translate(-50%,-50%) translate(-60px,40px) scale(1.1);} 50%{transform:translate(-50%,-50%) translate(50px,-60px) scale(0.9);} 75%{transform:translate(-50%,-50%) translate(-25px,-35px) scale(1.06);} }
  @keyframes drift-3 { 0%,100%{transform:translate(-50%,-50%) translate(0,0) scale(1);} 33%{transform:translate(-50%,-50%) translate(40px,50px) scale(1.08);} 66%{transform:translate(-50%,-50%) translate(-50px,-25px) scale(0.94);} }
  @keyframes drift-4 { 0%,100%{transform:translate(-50%,-50%) translate(0,0) scale(1);} 20%{transform:translate(-50%,-50%) translate(-50px,-40px) scale(1.12);} 60%{transform:translate(-50%,-50%) translate(-15px,50px) scale(1.04);} }
  @keyframes drift-5 { 0%,100%{transform:translate(-50%,-50%) translate(0,0) scale(1);} 30%{transform:translate(-50%,-50%) translate(45px,-35px) scale(1.06);} 60%{transform:translate(-50%,-50%) translate(-35px,45px) scale(0.96);} }
  @keyframes light-sweep { 0%{transform:translateX(-100%) rotate(-25deg);} 100%{transform:translateX(200%) rotate(-25deg);} }
  @keyframes frame-enter { from{opacity:0;transform:translateY(30px);} to{opacity:1;transform:translateY(0);} }
  @keyframes frame-exit { from{opacity:1;transform:translateY(0);} to{opacity:0;transform:translateY(-30px);} }
  @keyframes pop-in { from{opacity:0;transform:scale(0.95) translateY(4px);} to{opacity:1;transform:scale(1) translateY(0);} }
`;

// ─── PARTICLES ───
function Particles({ count = 40 }) {
  const ref = useRef(null);
  const pts = useRef([]);
  const raf = useRef(null);
  useEffect(() => {
    const c = ref.current; if (!c) return;
    const ctx = c.getContext("2d");
    const resize = () => { c.width = c.offsetWidth * (window.devicePixelRatio||1); c.height = c.offsetHeight * (window.devicePixelRatio||1); ctx.scale(window.devicePixelRatio||1, window.devicePixelRatio||1); };
    resize(); window.addEventListener("resize", resize);
    const cw = () => c.offsetWidth, ch = () => c.offsetHeight;
    pts.current = Array.from({length:count}, () => ({ x:Math.random()*cw(), y:Math.random()*ch(), vx:(Math.random()-0.5)*0.3, vy:(Math.random()-0.5)*0.3, r:Math.random()*1.5+0.5, gold:Math.random()<0.3 }));
    const draw = () => {
      ctx.clearRect(0,0,cw(),ch());
      const p = pts.current;
      for (let i=0;i<p.length;i++) for (let j=i+1;j<p.length;j++) { const dx=p[i].x-p[j].x,dy=p[i].y-p[j].y,d=Math.sqrt(dx*dx+dy*dy); if(d<120){ctx.beginPath();ctx.moveTo(p[i].x,p[i].y);ctx.lineTo(p[j].x,p[j].y);ctx.strokeStyle=`rgba(160,152,140,${(1-d/120)*0.08})`;ctx.lineWidth=0.5;ctx.stroke();}}
      for (const pt of p) { pt.x+=pt.vx;pt.y+=pt.vy;if(pt.x<0||pt.x>cw())pt.vx*=-1;if(pt.y<0||pt.y>ch())pt.vy*=-1;ctx.beginPath();ctx.arc(pt.x,pt.y,pt.r,0,Math.PI*2);ctx.fillStyle=pt.gold?"rgba(184,134,11,0.25)":"rgba(156,149,140,0.2)";ctx.fill();}
      raf.current = requestAnimationFrame(draw);
    };
    draw();
    return () => { window.removeEventListener("resize",resize); cancelAnimationFrame(raf.current); };
  }, [count]);
  return <canvas ref={ref} style={{position:"absolute",inset:0,width:"100%",height:"100%",zIndex:1,pointerEvents:"none"}} />;
}

function AmbientBackground() {
  const orbs = [
    {color:"rgba(184,134,11,0.09)",size:600,x:"10%",y:"15%",anim:"drift-1",dur:"26s"},
    {color:"rgba(184,134,11,0.07)",size:700,x:"80%",y:"50%",anim:"drift-2",dur:"32s"},
    {color:"rgba(160,152,144,0.08)",size:500,x:"55%",y:"5%",anim:"drift-3",dur:"22s"},
    {color:"rgba(184,134,11,0.06)",size:450,x:"90%",y:"10%",anim:"drift-4",dur:"28s"},
    {color:"rgba(160,152,144,0.07)",size:600,x:"20%",y:"80%",anim:"drift-5",dur:"30s"},
  ];
  return (
    <div style={{position:"absolute",inset:0,zIndex:0,overflow:"hidden"}}>
      {orbs.map((o,i) => <div key={i} style={{position:"absolute",left:o.x,top:o.y,width:o.size,height:o.size,borderRadius:"50%",background:`radial-gradient(circle,${o.color} 0%,transparent 60%)`,animation:`${o.anim} ${o.dur} ease-in-out infinite`,willChange:"transform"}} />)}
      <div style={{position:"absolute",inset:0,overflow:"hidden",pointerEvents:"none"}}><div style={{position:"absolute",top:"-50%",left:0,width:"40%",height:"200%",background:"linear-gradient(90deg,transparent,rgba(255,255,255,0.06),transparent)",animation:"light-sweep 16s ease-in-out infinite",willChange:"transform"}} /></div>
      <Particles count={40} />
    </div>
  );
}

// ─── ATOMS ───
function Dot({color,pulse,size=5}){return <span style={{position:"relative",display:"inline-flex",width:size,height:size}}>{pulse&&<span style={{position:"absolute",inset:0,borderRadius:"50%",background:color,animation:"pulse-dot 2s ease-in-out infinite"}}/>}<span style={{width:size,height:size,borderRadius:"50%",background:color}}/></span>;}
function Amt({value,size=13,weight=600}){const neg=value<0,zero=value===0,color=zero?C.textFaint:neg?C.red:C.green;return <span style={{fontSize:size,fontWeight:weight,fontFamily:num,color,letterSpacing:"-0.02em"}}>{neg?"−":""}€{Math.abs(value).toLocaleString("de-DE",{minimumFractionDigits:2})}</span>;}
function Label({children,color}){return <span style={{fontSize:10,fontWeight:600,fontFamily:ui,color:color||C.textMuted,textTransform:"uppercase",letterSpacing:"0.1em"}}>{children}</span>;}
function Card({children,style,accent}){return <div style={{background:C.page,border:`1px solid ${accent?C.borderAmber:C.border}`,borderRadius:10,boxShadow:C.shadow,overflow:"hidden",position:"relative",...style}}>{accent&&<div style={{position:"absolute",top:0,left:20,right:20,height:1,background:`linear-gradient(90deg,transparent,rgba(184,134,11,0.2),transparent)`}}/>}{children}</div>;}
function SparkBars({data,height=40,color=C.red}){const max=Math.max(...data.map(Math.abs));return <div style={{display:"flex",alignItems:"flex-end",gap:3,height}}>{data.map((v,i)=><div key={i} style={{width:5,borderRadius:1.5,height:max===0?2:Math.max(2,(Math.abs(v)/max)*height),background:color,opacity:0.12+(i/data.length)*0.3,transformOrigin:"bottom",animation:`bar-grow 0.5s ease ${i*0.04}s both`}}/>)}</div>;}
function SectionTitle({children,right}){return <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:12}}><span style={{fontSize:14,fontWeight:700,color:C.text}}>{children}</span>{right}</div>;}
function Tabs({tabs,active,onChange}){return <div style={{display:"inline-flex",gap:1,background:C.canvas,borderRadius:8,padding:3,border:`1px solid ${C.border}`}}>{tabs.map(t=><button key={t.id} onClick={()=>onChange(t.id)} style={{padding:"6px 16px",fontSize:12,fontWeight:active===t.id?600:400,fontFamily:ui,borderRadius:6,border:"none",cursor:"pointer",color:active===t.id?C.text:C.textMuted,background:active===t.id?C.page:"transparent",boxShadow:active===t.id?C.shadow:"none",transition:"all 0.2s",display:"flex",alignItems:"center",gap:6}}>{t.label}{t.count!==undefined&&<span style={{fontSize:10,fontWeight:600,fontFamily:num,color:t.countColor||C.textMuted,background:t.countBg||C.canvas,borderRadius:4,padding:"1px 6px",lineHeight:"16px"}}>{t.count}</span>}</button>)}</div>;}
function TH({cols,headers}){return <div style={{display:"grid",gridTemplateColumns:cols,padding:"0 22px",borderBottom:`1.5px solid ${C.borderStrong}`}}>{headers.map(h=><div key={h.label} style={{padding:"11px 0 10px",fontSize:11,fontWeight:600,fontFamily:ui,color:C.thText,letterSpacing:"0.02em",textAlign:h.align||"left"}}>{h.label}</div>)}</div>;}

// ─── COLLAPSIBLE SECTION ───
function Collapsible({ title, right, defaultOpen = false, children }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div style={{ borderBottom: `1px solid ${C.border}` }}>
      <button onClick={() => setOpen(!open)} style={{
        display: "flex", alignItems: "center", justifyContent: "space-between",
        width: "100%", padding: "16px 0", fontSize: 13, fontWeight: 600,
        fontFamily: ui, border: "none", cursor: "pointer", background: "transparent",
        color: C.text, textAlign: "left",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <span style={{ fontSize: 10, color: C.textMuted, transform: open ? "rotate(90deg)" : "rotate(0)", transition: "transform 0.2s" }}>›</span>
          {title}
          {right && <span style={{ fontWeight: 400, fontSize: 12, color: C.textMuted, marginLeft: 8 }}>{right}</span>}
        </div>
      </button>
      {open && <div style={{ paddingBottom: 16 }}>{children}</div>}
    </div>
  );
}

// ─── STATUS BADGE ───
function StatusBadge({ label, color = C.green }) {
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 5, fontSize: 11, fontWeight: 500, color }}>
      <span style={{ width: 5, height: 5, borderRadius: "50%", background: color }} />
      {label}
    </span>
  );
}


// ═══════════════════════════════════════
//  SIDEBAR
// ═══════════════════════════════════════
function FloatingSidebar({ screen, setScreen, onProfileClick }) {
  const [expanded, setExpanded] = useState({ accounting: true, company: false, tomorrow: false });
  const toggle = (key) => setExpanded(p => ({ ...p, [key]: !p[key] }));

  // Auto-expand correct section when navigating
  useEffect(() => {
    const companyItems = ["company-details","contacts","team"];
    const tomorrowItems = ["ai-chat","forecast"];
    if (companyItems.includes(screen)) setExpanded(p => ({...p, company: true}));
    if (tomorrowItems.includes(screen)) setExpanded(p => ({...p, tomorrow: true}));
  }, [screen]);

  const navSections = [
    { id:"accounting", label:"Accounting", icon:"📊", items:[
      {id:"today",label:"Today"},{id:"documents",label:"Documents"},
      {id:"cashflow",label:"Cashflow"},{id:"accountant",label:"Accountant"},
      {id:"vat",label:"VAT",soon:true},{id:"reports",label:"Reports",soon:true},
    ]},
    { id:"company", label:"Company", icon:"👥", items:[
      {id:"company-details",label:"Company Details"},{id:"contacts",label:"Contacts"},{id:"team",label:"Team"},
    ]},
    { id:"tomorrow", label:"Tomorrow", icon:"✦", items:[
      {id:"ai-chat",label:"AI Chat"},{id:"forecast",label:"Forecast",soon:true},
    ]},
  ];

  return (
    <div style={{
      width:220, background:C.glass,
      backdropFilter:"blur(60px)",WebkitBackdropFilter:"blur(60px)",
      border:`1px solid ${C.glassBorder}`,borderRadius:16,
      boxShadow:`0 8px 32px rgba(0,0,0,0.06),0 1px 2px rgba(0,0,0,0.03),inset 0 1px 0 rgba(255,255,255,0.5)`,
      display:"flex",flexDirection:"column",flexShrink:0,overflow:"hidden",
    }}>
      <div style={{padding:"16px 16px 18px",display:"flex",alignItems:"center",gap:10}}>
        <div style={{width:26,height:26,borderRadius:6,border:`1.5px solid rgba(0,0,0,0.08)`,background:"rgba(255,255,255,0.5)",display:"flex",alignItems:"center",justifyContent:"center",fontSize:10,fontWeight:700,fontFamily:num,color:C.amber}}>[#]</div>
        <span style={{fontSize:16,fontWeight:700,letterSpacing:"-0.02em",color:C.text}}>Dokus<span style={{color:C.amber,fontSize:6,verticalAlign:"super",marginLeft:2}}>•</span></span>
      </div>
      <nav style={{flex:1,padding:"0 8px",display:"flex",flexDirection:"column",gap:2,overflow:"auto"}}>
        {navSections.map(section => (
          <div key={section.id}>
            <button onClick={()=>toggle(section.id)} style={{display:"flex",alignItems:"center",justifyContent:"space-between",width:"100%",padding:"6px 10px",fontSize:12.5,fontWeight:600,fontFamily:ui,borderRadius:8,border:"none",cursor:"pointer",color:section.items.some(i=>i.id===screen)?C.amber:C.text,background:"transparent",transition:"color 0.15s",textAlign:"left"}}>
              <div style={{display:"flex",alignItems:"center",gap:7}}><span style={{fontSize:12}}>{section.icon}</span>{section.label}</div>
              <span style={{fontSize:10,color:C.textFaint,transform:expanded[section.id]?"rotate(90deg)":"rotate(0deg)",transition:"transform 0.2s"}}>›</span>
            </button>
            {expanded[section.id] && (
              <div style={{marginLeft:14,borderLeft:`1px solid rgba(0,0,0,0.06)`,marginTop:2,marginBottom:6,display:"flex",flexDirection:"column",gap:1}}>
                {section.items.map(item => (
                  <button key={item.id} onClick={()=>!item.soon&&setScreen(item.id)} style={{display:"flex",alignItems:"center",gap:8,padding:"5px 11px",fontSize:12.5,fontWeight:screen===item.id?600:400,fontFamily:ui,borderRadius:"0 7px 7px 0",border:"none",cursor:item.soon?"default":"pointer",textAlign:"left",width:"100%",color:item.soon?C.textFaint:screen===item.id?C.text:C.textMuted,background:screen===item.id?"rgba(255,255,255,0.55)":"transparent",boxShadow:screen===item.id?"0 1px 3px rgba(0,0,0,0.04)":"none",borderLeft:screen===item.id?`2px solid ${C.amber}`:"2px solid transparent",marginLeft:-1,transition:"all 0.15s",opacity:item.soon?0.5:1}}>
                    {item.label}
                    {item.soon&&<span style={{fontSize:8,fontWeight:500,color:C.textFaint,background:"rgba(0,0,0,0.03)",borderRadius:3,padding:"1px 5px",marginLeft:"auto"}}>Soon</span>}
                  </button>
                ))}
              </div>
            )}
          </div>
        ))}
      </nav>
      <div onClick={onProfileClick} style={{padding:"10px 12px",borderTop:`1px solid rgba(0,0,0,0.06)`,display:"flex",alignItems:"center",gap:8,cursor:"pointer"}}>
        <div style={{width:24,height:24,borderRadius:5,background:C.amberWhisper,border:`1px solid ${C.borderAmber}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:9,fontWeight:700,color:C.amber}}>I</div>
        <div style={{flex:1,minWidth:0}}>
          <div style={{fontSize:10,fontWeight:600,color:C.text}}>INVOID VISION</div>
          <div style={{fontSize:8,fontFamily:num,color:C.textMuted}}>BE0777.887.045</div>
        </div>
        <div style={{width:22,height:22,borderRadius:6,background:`linear-gradient(135deg, ${C.amberSoft}, rgba(184,134,11,0.15))`,border:`1px solid ${C.borderAmber}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:7,fontWeight:700,fontFamily:num,color:C.amber}}>AK</div>
      </div>
    </div>
  );
}

// ─── USER PROFILE POPOVER ───
function ProfilePopover({ onClose, onNavigate }) {
  return (
    <>
      <div onClick={onClose} style={{ position:"fixed",inset:0,zIndex:99 }} />
      <div style={{
        position:"absolute",bottom:60,left:12,width:220,
        background:C.page,border:`1px solid ${C.border}`,borderRadius:12,
        boxShadow:"0 12px 40px rgba(0,0,0,0.12)",zIndex:100,overflow:"hidden",
        animation:"pop-in 0.15s ease",
      }}>
        <div style={{padding:"14px 16px",borderBottom:`1px solid ${C.border}`,display:"flex",alignItems:"center",gap:10}}>
          <div style={{width:32,height:32,borderRadius:8,background:`linear-gradient(135deg, ${C.amberSoft}, rgba(184,134,11,0.15))`,border:`1px solid ${C.borderAmber}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:11,fontWeight:700,fontFamily:num,color:C.amber}}>AK</div>
          <div style={{flex:1,minWidth:0}}>
            <div style={{fontSize:12,fontWeight:600,color:C.text}}>Artem Kuznetsov</div>
            <div style={{fontSize:10,color:C.textMuted}}>artem@invoid.vision</div>
          </div>
          <span style={{fontSize:9,fontWeight:600,fontFamily:num,color:C.amber,background:C.amberSoft,borderRadius:4,padding:"2px 6px"}}>Core</span>
        </div>
        {[{label:"Profile",screen:"profile"},{label:"Appearance",screen:null}].map((item,i) => (
          <div key={i} onClick={() => { if(item.screen){ onNavigate(item.screen); onClose(); } }} style={{padding:"10px 16px",fontSize:12.5,fontWeight:500,color:C.text,cursor:"pointer",transition:"background 0.15s",display:"flex",alignItems:"center",justifyContent:"space-between"}}
            onMouseEnter={e=>e.currentTarget.style.background=C.warm}
            onMouseLeave={e=>e.currentTarget.style.background="transparent"}
          ><span>{item.label}</span><span style={{fontSize:12,color:C.textFaint}}>›</span></div>
        ))}
        <div style={{borderTop:`1px solid ${C.border}`,padding:"10px 16px",fontSize:12.5,fontWeight:500,color:C.red,cursor:"pointer"}}>Log Out</div>
      </div>
    </>
  );
}


// ═══════════════════════════════════════
//  SCREEN META
// ═══════════════════════════════════════
const screenMeta = {
  "today":{title:"Today",sub:"Financial overview"},
  "documents":{title:"Documents",sub:"Invoices & receipts"},
  "document-detail":{title:"Document Review",sub:""},
  "cashflow":{title:"Cashflow",sub:"Money in & out"},
  "accountant":{title:"Accountant",sub:"Export & compliance"},
  "company-details":{title:"Company Settings",sub:"INVOID VISION · BE0777887045"},
  "contacts":{title:"Contacts",sub:"Vendors & clients"},
  "team":{title:"Team",sub:"Access & permissions"},
  "ai-chat":{title:"AI Chat",sub:"Intelligence layer"},
  "profile":{title:"Profile",sub:"Account settings"},
};


// ═══════════════════════════════════════
//  TODAY
// ═══════════════════════════════════════
function TodayScreen() {
  return (
    <div style={{display:"flex",flexDirection:"column",gap:20}}>
      <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:12}}>
        <Card style={{padding:"20px 22px"}} accent><Label color={C.red}>Overdue</Label><div style={{fontSize:28,fontWeight:700,fontFamily:num,color:C.red,letterSpacing:"-0.04em",lineHeight:1,marginTop:10}}>−€4.016,21</div><div style={{fontSize:12,color:C.textMuted,marginTop:10}}>8 invoices past due</div></Card>
        <Card style={{padding:"20px 22px"}}><Label color={C.amber}>Due this week</Label><div style={{fontSize:28,fontWeight:700,fontFamily:num,color:C.amber,letterSpacing:"-0.04em",lineHeight:1,marginTop:10}}>€289,00</div><div style={{fontSize:12,color:C.textMuted,marginTop:10}}>1 invoice · Feb 20</div></Card>
      </div>
      <Card style={{padding:"14px 20px"}} accent>
        <div style={{display:"flex",alignItems:"center",gap:14}}>
          <div style={{width:8,height:8,borderRadius:"50%",background:C.amber,flexShrink:0}} />
          <div style={{flex:1}}><div style={{fontSize:13,fontWeight:600,color:C.text}}>Document needs review</div><div style={{fontSize:12,color:C.textMuted,marginTop:2}}><span style={{fontFamily:num,fontSize:11}}>Jan 27</span><span style={{margin:"0 6px",color:C.textFaint}}>·</span>Vendor unrecognized</div></div>
          <button style={{fontSize:12,fontWeight:600,fontFamily:ui,color:"#fff",background:C.amber,border:"none",borderRadius:7,padding:"7px 18px",cursor:"pointer",boxShadow:"0 1px 3px rgba(184,134,11,0.25)"}}>Review</button>
        </div>
      </Card>
      <div>
        <SectionTitle right={<span style={{fontSize:12,color:C.textMuted,cursor:"pointer"}}>View all →</span>}>Recent</SectionTitle>
        <Card>{[{v:"KBC Bank",amt:-289,date:"Feb 14",src:"PDF"},{v:"Unknown",amt:-9.64,date:"Feb 11",src:"PEPPOL"},{v:"Donckers Schoten",amt:-1306.12,date:"Jan 30",src:"PDF"},{v:"Tesla Belgium",amt:-9.99,date:"Jan 28",src:"PEPPOL"},{v:"KBC Bank",amt:-962.52,date:"Jan 21",src:"PDF"}].map((d,i,a)=><div key={i} style={{padding:"11px 18px",display:"flex",alignItems:"center",borderBottom:i<a.length-1?`1px solid ${C.border}`:"none",cursor:"pointer",transition:"background 0.15s"}} onMouseEnter={e=>e.currentTarget.style.background=C.warm} onMouseLeave={e=>e.currentTarget.style.background="transparent"}><div style={{width:26,height:26,borderRadius:5,background:d.src==="PEPPOL"?C.amberWhisper:C.canvas,border:`1px solid ${d.src==="PEPPOL"?C.borderAmber:C.border}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:7,fontWeight:600,fontFamily:num,color:d.src==="PEPPOL"?C.amber:C.textMuted,flexShrink:0}}>{d.src==="PEPPOL"?"PP":"PDF"}</div><div style={{flex:1,marginLeft:12,minWidth:0}}><div style={{fontSize:13,fontWeight:500,color:C.text}}>{d.v}</div><span style={{fontSize:10,fontFamily:num,color:C.textMuted}}>{d.date}</span></div><Amt value={d.amt} size={12}/></div>)}</Card>
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  DOCUMENTS
// ═══════════════════════════════════════
const ALL_DOCS=[{v:"KBC Bank",ref:"384421507",amt:-289,date:"Feb 14",st:"ok",src:"PDF"},{v:"Unknown",ref:"peppol-7ff798f8",amt:-9.64,date:"Feb 11",st:"ok",src:"PEPPOL"},{v:"Donckers Schoten",ref:"100111009120",amt:-1306.12,date:"Jan 30",st:"ok",src:"PDF"},{v:"Tesla Belgium",ref:"peppol-439380c9",amt:-9.99,date:"Jan 28",st:"ok",src:"PEPPOL"},{v:"Unknown vendor",ref:"BE0428927169",amt:-798.60,date:"Jan 27",st:"warn",src:"PDF"},{v:"KBC Bank",ref:"00010001BE26",amt:-962.52,date:"Jan 21",st:"ok",src:"PDF"},{v:"Tesla Belgium",ref:"peppol-71b40a13",amt:-346.97,date:"Jan 14",st:"ok",src:"PEPPOL"},{v:"Coolblue België",ref:"BE0867686774",amt:-249.01,date:"Jan 11",st:"ok",src:"PDF"},{v:"SRL Accounting & Tax",ref:"INVOID-2026-01",amt:-798.6,date:"Jan 2",st:"ok",src:"PDF"},{v:"KBC Bank",ref:"2504773248",amt:-45,date:"Dec 31",st:"ok",src:"PDF"}];

const DOC_DATA={
  "384421507":{vendor:"KBC Bank NV",vendorConf:"high",address:"Havenlaan 2, 1080 Brussels",ref:"384421507",refConf:"high",issueDate:"2026-02-14",issueDateConf:"high",dueDate:"2026-02-28",dueDateConf:"high",subtotal:239.67,vat:49.33,total:289.00,vatRate:"21%",lines:[{desc:"Insurance premium — Q1 2026",amt:289.00}],origin:"PDF",needsContact:false,pageTitle:"KBC",pageSub:"KBC Bank NV",iban:"BE39 7350 0001 0000",bic:"KREDBEBB",
    sources:[{type:"PDF",name:"KBC_384421507.pdf",date:"Feb 14"},{type:"PEPPOL",name:"UBL Invoice (matched)",date:"Feb 14"}],
    transactions:[{date:"Feb 15",amt:-289.00,method:"Bank transfer",ref:"KBC-AUTO-2026021501"}],
  },
  "BE0428927169":{vendor:null,vendorConf:"missing",address:"Chemin Bodinet 42, Soignies 7060",ref:"20260050",refConf:"high",issueDate:"2026-01-02",issueDateConf:"high",dueDate:"2026-01-16",dueDateConf:"high",subtotal:660.00,vat:138.60,total:798.60,vatRate:"21%",lines:[{desc:"Comptabilité & prestations - 4ème trimestre 2025",amt:600.00},{desc:"Gestion salaire dirigeant",amt:60.00}],origin:"PDF",needsContact:true,pageTitle:"ats.",pageSub:"SRL Accounting & Tax Solutions",iban:"BE86 3632 0614 5450",bic:"BBRUBEBB",
    sources:[{type:"PDF",name:"facture_20260050.pdf",date:"Jan 27"}],
    transactions:[],
  },
  "peppol-439380c9":{vendor:"Tesla Belgium BVBA",vendorConf:"high",address:"Brussels, Belgium",ref:"peppol-439380c9",refConf:"high",issueDate:"2026-01-28",issueDateConf:"high",dueDate:null,dueDateConf:"low",subtotal:8.26,vat:1.73,total:9.99,vatRate:"21%",lines:[{desc:"Premium Connectivity — Monthly",amt:9.99}],origin:"PEPPOL",needsContact:false,pageTitle:"Tesla",pageSub:"Invoice",iban:null,bic:null,
    sources:[{type:"PEPPOL",name:"UBL Invoice",date:"Jan 28"}],
    transactions:[],
  },
  "peppol-71b40a13":{vendor:"Tesla Belgium BVBA",vendorConf:"high",address:"Brussels, Belgium",ref:"peppol-71b40a13",refConf:"high",issueDate:"2026-01-14",issueDateConf:"high",dueDate:"2026-02-13",dueDateConf:"high",subtotal:286.75,vat:60.22,total:346.97,vatRate:"21%",lines:[{desc:"Supercharging — December 2025",amt:346.97}],origin:"PEPPOL",needsContact:false,pageTitle:"Tesla",pageSub:"Invoice",iban:null,bic:null,
    sources:[{type:"PEPPOL",name:"UBL Invoice",date:"Jan 14"},{type:"PDF",name:"tesla_dec_charging.pdf",date:"Jan 15"}],
    transactions:[{date:"Jan 20",amt:-346.97,method:"Direct debit",ref:"TESLA-DD-20260120"}],
  },
};

function getDocData(doc) {
  if (DOC_DATA[doc.ref]) return DOC_DATA[doc.ref];
  return {
    vendor:doc.v,vendorConf:doc.st==="warn"?"medium":"high",address:null,ref:doc.ref,refConf:"high",
    issueDate:"2026-01-15",issueDateConf:"high",dueDate:"2026-02-14",dueDateConf:"high",
    subtotal:Math.abs(doc.amt)*0.826,vat:Math.abs(doc.amt)*0.174,total:Math.abs(doc.amt),vatRate:"21%",
    lines:[{desc:"Services rendered",amt:Math.abs(doc.amt)}],origin:doc.src,needsContact:false,
    pageTitle:doc.v,pageSub:"Invoice",iban:null,bic:null,
    sources:[{type:doc.src,name:doc.src==="PEPPOL"?"UBL Invoice":`${doc.ref}.pdf`,date:doc.date}],
    transactions:[],
  };
}

// ─── CONFIDENCE DOT ───
function ConfDot({ level }) {
  const colors = { high: C.green, medium: C.amber, low: C.red, missing: C.red };
  return <span style={{ width: 5, height: 5, borderRadius: "50%", background: colors[level] || C.textFaint, flexShrink: 0, display: "inline-block" }} />;
}

// ─── INSPECTOR ROW ───
function InspRow({ label, value, mono, confidence, editable, warn }) {
  return (
    <div style={{ display: "flex", alignItems: "flex-start", padding: "7px 0", gap: 4 }}>
      {confidence && <div style={{ paddingTop: 5 }}><ConfDot level={confidence} /></div>}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 10, color: C.textMuted, marginBottom: 2 }}>{label}</div>
        <div style={{
          fontSize: 12.5, fontWeight: 500, fontFamily: mono ? num : ui,
          color: warn ? C.amber : value ? C.text : C.textFaint,
          padding: editable ? "4px 8px" : "0",
          background: editable ? C.canvas : "transparent",
          border: editable ? `1px solid ${warn ? C.borderAmber : C.border}` : "none",
          borderRadius: editable ? 6 : 0,
        }}>{value || "—"}</div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════
//  DOCUMENT DETAIL MODE — Full screen, own windows
// ═══════════════════════════════════════
function DocumentDetailMode({ docs, initialIdx, onExit }) {
  const [idx, setIdx] = useState(initialIdx);
  const [cashDir, setCashDir] = useState("out");
  const doc = docs[idx];
  const isReview = doc.st === "warn";
  const dd = getDocData(doc);

  // Keyboard nav
  useEffect(() => {
    const h = e => {
      if (e.key === "ArrowDown" || e.key === "j") { e.preventDefault(); setIdx(i => Math.min(i + 1, docs.length - 1)); }
      if (e.key === "ArrowUp" || e.key === "k") { e.preventDefault(); setIdx(i => Math.max(i - 1, 0)); }
      if (e.key === "Escape") onExit();
    };
    window.addEventListener("keydown", h);
    return () => window.removeEventListener("keydown", h);
  }, [docs.length, onExit]);

  return (
    <div style={{ display: "flex", height: "100%", background: C.bg, fontFamily: ui, color: C.text, position: "relative" }}>
      <AmbientBackground />
      <div style={{ display: "flex", flex: 1, gap: 10, padding: 10, position: "relative", zIndex: 1 }}>

        {/* ══ LEFT: Document Queue ══ */}
        <div style={{
          width: 220, background: C.glass, backdropFilter: "blur(60px)", WebkitBackdropFilter: "blur(60px)",
          border: `1px solid ${C.glassBorder}`, borderRadius: 16,
          boxShadow: "0 8px 32px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.03), inset 0 1px 0 rgba(255,255,255,0.5)",
          display: "flex", flexDirection: "column", flexShrink: 0, overflow: "hidden",
        }}>
          <div style={{ padding: "12px 14px 10px", display: "flex", alignItems: "center", gap: 6, borderBottom: "1px solid rgba(0,0,0,0.06)" }}>
            <button onClick={onExit} style={{ display: "flex", alignItems: "center", gap: 4, fontSize: 12, fontWeight: 500, color: C.amber, background: "none", border: "none", cursor: "pointer", fontFamily: ui, padding: 0 }}>
              <svg width="7" height="12" viewBox="0 0 7 12" fill="none"><path d="M6 1L1.5 6L6 11" stroke={C.amber} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
              All docs
            </button>
            <span style={{ marginLeft: "auto", fontSize: 10, fontFamily: num, color: C.textFaint }}>{idx + 1}/{docs.length}</span>
          </div>
          <div style={{ flex: 1, overflow: "auto" }}>
            {docs.map((d, i) => (
              <div key={i} onClick={() => setIdx(i)} style={{
                padding: "10px 14px", display: "flex", alignItems: "center", gap: 8,
                cursor: "pointer", transition: "all 0.1s",
                background: i === idx ? C.warm : "transparent",
                borderRight: i === idx ? `2px solid ${C.amber}` : "2px solid transparent",
                borderBottom: "1px solid rgba(0,0,0,0.03)",
              }}>
                <Dot color={d.st === "warn" ? C.amber : C.green} size={5} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 11, fontWeight: i === idx ? 600 : 400, color: C.text, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{d.v}</div>
                  <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted, marginTop: 1 }}>{d.date}</div>
                </div>
                <span style={{ fontSize: 11, fontFamily: num, fontWeight: 500, color: C.text, flexShrink: 0 }}>€{Math.abs(d.amt).toLocaleString("de-DE", { minimumFractionDigits: 0, maximumFractionDigits: 0 })}</span>
              </div>
            ))}
          </div>
          <div style={{ padding: "7px 14px", borderTop: "1px solid rgba(0,0,0,0.06)", display: "flex", gap: 6, justifyContent: "center" }}>
            <span style={{ fontSize: 9, color: C.textFaint, display: "flex", alignItems: "center", gap: 3 }}>
              <span style={{ padding: "1px 4px", border: `1px solid ${C.textFaint}`, borderRadius: 3, fontFamily: num, fontSize: 8 }}>↑</span>
              <span style={{ padding: "1px 4px", border: `1px solid ${C.textFaint}`, borderRadius: 3, fontFamily: num, fontSize: 8 }}>↓</span>
              navigate
            </span>
          </div>
        </div>

        {/* ══ RIGHT: Preview + Inspector ══ */}
        <div style={{
          flex: 1, background: C.glassContent, backdropFilter: "blur(60px)", WebkitBackdropFilter: "blur(60px)",
          border: `1px solid ${C.glassBorder}`, borderRadius: 16,
          boxShadow: "0 8px 32px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.03), inset 0 1px 0 rgba(255,255,255,0.5)",
          display: "flex", flexDirection: "column", overflow: "hidden",
        }}>
          {/* Title bar */}
          <div style={{ padding: "10px 18px", borderBottom: "1px solid rgba(0,0,0,0.05)", background: C.glassHeader, display: "flex", alignItems: "center", justifyContent: "space-between", flexShrink: 0 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <span style={{ fontSize: 14, fontWeight: 700, color: C.text, letterSpacing: "-0.02em" }}>{dd.vendor || doc.v || "Unknown"}</span>
              <span style={{ fontSize: 11, fontFamily: num, color: C.textMuted }}>€{dd.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              {dd.transactions.length > 0 && <span style={{ fontSize: 9, fontWeight: 600, fontFamily: num, color: C.green, background: C.greenSoft, borderRadius: 4, padding: "2px 7px" }}>Paid</span>}
              <Dot color={isReview ? C.amber : C.green} size={5} />
              <span style={{ fontSize: 10, fontWeight: 600, color: isReview ? C.amber : C.green }}>{isReview ? "Needs review" : "Confirmed"}</span>
            </div>
          </div>

          {/* Split: Preview | Inspector */}
          <div style={{ flex: 1, display: "grid", gridTemplateColumns: "1fr 272px", overflow: "hidden" }}>

            {/* ── Preview ── */}
            <div style={{ overflow: "auto", padding: 20, display: "flex", justifyContent: "center" }}>
              <div style={{
                width: "100%", maxWidth: 480, minHeight: 480,
                background: "#fff", borderRadius: 3,
                boxShadow: "0 2px 16px rgba(0,0,0,0.07), 0 0 1px rgba(0,0,0,0.1)",
                padding: "36px 40px", position: "relative", alignSelf: "flex-start",
              }}>
                <div style={{ position: "absolute", top: 12, right: 12, fontSize: 8, fontWeight: 600, fontFamily: num, color: dd.origin === "PEPPOL" ? C.amber : C.textMuted, background: dd.origin === "PEPPOL" ? C.amberWhisper : C.canvas, border: `1px solid ${dd.origin === "PEPPOL" ? C.borderAmber : C.border}`, borderRadius: 3, padding: "2px 6px" }}>{dd.origin}</div>
                <div style={{ fontSize: 19, fontWeight: 700, color: C.text, letterSpacing: "-0.02em", marginBottom: 2 }}>{dd.pageTitle}</div>
                <div style={{ fontSize: 10, color: C.textMuted, marginBottom: 24 }}>{dd.pageSub}</div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 18, marginBottom: 22, fontSize: 10, color: C.textSec, lineHeight: 1.6 }}>
                  <div>
                    <div style={{ fontSize: 8, fontWeight: 600, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 3 }}>From</div>
                    <div style={{ fontWeight: 600, color: C.text }}>{dd.vendor || "Unknown vendor"}</div>
                    {dd.address && <div>{dd.address}</div>}
                  </div>
                  <div>
                    <div style={{ fontSize: 8, fontWeight: 600, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 3 }}>To</div>
                    <div style={{ fontWeight: 600, color: C.text }}>BV INVOID VISION</div>
                    <div>Balegemstraat, 17/7</div>
                    <div>9860 Oosterzele</div>
                    <div style={{ fontFamily: num, fontSize: 9, marginTop: 1 }}>BE 0777.887.045</div>
                  </div>
                </div>
                <div style={{ display: "flex", gap: 18, padding: "7px 0", borderTop: `1px solid ${C.border}`, borderBottom: `1px solid ${C.border}`, marginBottom: 18 }}>
                  {[{ l: "Invoice", v: dd.ref }, { l: "Date", v: dd.issueDate }, { l: "Due", v: dd.dueDate || "—" }].map((f, i) => (
                    <div key={i}><div style={{ fontSize: 8, fontWeight: 600, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.06em" }}>{f.l}</div><div style={{ fontSize: 10, fontFamily: num, fontWeight: 500, color: C.text, marginTop: 2 }}>{f.v}</div></div>
                  ))}
                </div>
                <div style={{ marginBottom: 18 }}>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 68px", padding: "4px 0", borderBottom: `1px solid ${C.borderStrong}` }}>
                    <span style={{ fontSize: 8, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.06em" }}>Description</span>
                    <span style={{ fontSize: 8, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.06em", textAlign: "right" }}>Amount</span>
                  </div>
                  {dd.lines.map((line, li) => (
                    <div key={li} style={{ display: "grid", gridTemplateColumns: "1fr 68px", padding: "8px 0", borderBottom: `1px solid ${C.border}` }}>
                      <span style={{ fontSize: 10, color: C.text }}>{line.desc}</span>
                      <span style={{ fontSize: 10, fontFamily: num, color: C.text, textAlign: "right" }}>€{line.amt.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
                    </div>
                  ))}
                </div>
                <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 3 }}>
                  {[{ l: "Subtotal", v: dd.subtotal }, { l: `VAT ${dd.vatRate}`, v: dd.vat }].map((r, ri) => (
                    <div key={ri} style={{ display: "flex", gap: 18, fontSize: 10 }}>
                      <span style={{ color: C.textMuted }}>{r.l}</span>
                      <span style={{ fontFamily: num, color: C.text, width: 68, textAlign: "right" }}>€{r.v.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
                    </div>
                  ))}
                  <div style={{ display: "flex", gap: 18, fontSize: 12, fontWeight: 700, marginTop: 3, paddingTop: 4, borderTop: `1.5px solid ${C.text}` }}>
                    <span>Total</span>
                    <span style={{ fontFamily: num, width: 68, textAlign: "right" }}>€{dd.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
                  </div>
                </div>
                {dd.iban && (
                  <div style={{ marginTop: 24, paddingTop: 12, borderTop: `1px solid ${C.border}`, fontSize: 9, color: C.textMuted, fontFamily: num }}>
                    {dd.iban}{dd.bic && <span style={{ marginLeft: 8 }}>BIC: {dd.bic}</span>}
                  </div>
                )}
              </div>
            </div>

            {/* ── Inspector ── */}
            <div style={{ borderLeft: `1px solid ${C.border}`, overflow: "auto", display: "flex", flexDirection: "column" }}>
              {/* Status */}
              <div style={{ padding: "11px 14px", borderBottom: `1px solid ${C.border}`, background: isReview ? C.amberWhisper : C.greenSoft }}>
                <div style={{ display: "flex", alignItems: "center", gap: 5, marginBottom: 2 }}>
                  <Dot color={isReview ? C.amber : C.green} pulse={isReview} size={6} />
                  <span style={{ fontSize: 12, fontWeight: 700, color: C.text }}>{isReview ? "Review required" : "Confirmed"}</span>
                </div>
                <div style={{ fontSize: 10, color: C.textMuted }}>{isReview ? "AI extracted. Verify below." : `Locked · ${doc.date}`}</div>
              </div>

              <div style={{ flex: 1, padding: "8px 14px", display: "flex", flexDirection: "column", gap: 0, overflow: "auto" }}>
                {/* Contact */}
                <div style={{ paddingBottom: 10, borderBottom: `1px solid ${C.border}` }}>
                  <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>Contact</div>
                  {dd.needsContact ? (
                    <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "8px 10px", background: C.amberWhisper, border: `1px solid ${C.borderAmber}`, borderRadius: 8, cursor: "pointer" }}>
                      <Dot color={C.amber} size={6} />
                      <div style={{ flex: 1 }}><div style={{ fontSize: 11, fontWeight: 600, color: C.text }}>Who issued this?</div><div style={{ fontSize: 9, color: C.textMuted }}>Tap to identify</div></div>
                      <span style={{ fontSize: 12, color: C.textFaint }}>›</span>
                    </div>
                  ) : (
                    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                      <div style={{ width: 26, height: 26, borderRadius: 7, flexShrink: 0, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 8, fontWeight: 700, fontFamily: num, color: C.amber }}>{(dd.vendor || "??").split(" ").map(w => w[0]).join("").slice(0, 2)}</div>
                      <div>
                        <div style={{ fontSize: 11, fontWeight: 600, color: C.text }}>{dd.vendor}</div>
                        {doc.ref?.startsWith("peppol") && <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>via PEPPOL</div>}
                      </div>
                    </div>
                  )}
                </div>

                {/* Cashflow — only in review */}
                {isReview && (
                  <div style={{ padding: "10px 0", borderBottom: `1px solid ${C.border}` }}>
                    <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>Cashflow</div>
                    <div style={{ display: "flex", gap: 2, background: C.canvas, borderRadius: 7, padding: 2 }}>
                      {["In", "Out"].map(d => (
                        <button key={d} onClick={() => setCashDir(d.toLowerCase())} style={{ flex: 1, padding: "6px 0", fontSize: 11, fontWeight: cashDir === d.toLowerCase() ? 600 : 400, fontFamily: ui, borderRadius: 5, border: "none", cursor: "pointer", color: cashDir === d.toLowerCase() ? "#fff" : C.textMuted, background: cashDir === d.toLowerCase() ? (d === "In" ? C.green : C.amber) : "transparent", transition: "all 0.15s" }}>{d}</button>
                      ))}
                    </div>
                  </div>
                )}

                {/* Invoice Details */}
                <div style={{ padding: "10px 0", borderBottom: `1px solid ${C.border}` }}>
                  <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 2 }}>Invoice Details</div>
                  <InspRow label="Invoice Number" value={dd.ref} mono confidence={dd.refConf} editable={isReview} />
                  <InspRow label="Issue Date" value={dd.issueDate} mono confidence={dd.issueDateConf} editable={isReview} />
                  <InspRow label="Due Date" value={dd.dueDate} mono confidence={dd.dueDateConf} editable={isReview} warn={!dd.dueDate} />
                </div>

                {/* Amounts */}
                <div style={{ padding: "10px 0", borderBottom: `1px solid ${C.border}` }}>
                  <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 2 }}>Amounts</div>
                  <InspRow label="Subtotal" value={`€${dd.subtotal.toLocaleString("de-DE", { minimumFractionDigits: 2 })}`} mono confidence="high" />
                  <InspRow label="VAT" value={`€${dd.vat.toLocaleString("de-DE", { minimumFractionDigits: 2 })}`} mono confidence="high" />
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "8px 0 2px", marginTop: 2, borderTop: `1px solid ${C.borderStrong}` }}>
                    <span style={{ fontSize: 11, fontWeight: 700, color: C.text }}>Total</span>
                    <span style={{ fontSize: 14, fontWeight: 700, fontFamily: num, color: C.text }}>€{dd.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
                  </div>
                </div>

                {/* Sources */}
                <div style={{ padding: "10px 0", borderBottom: `1px solid ${C.border}` }}>
                  <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 6 }}>Sources</div>
                  {dd.sources.map((s, si) => (
                    <div key={si} style={{ display: "flex", alignItems: "center", gap: 7, padding: "4px 0" }}>
                      <span style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: s.type === "PEPPOL" ? C.amber : C.textMuted, background: s.type === "PEPPOL" ? C.amberWhisper : C.canvas, border: `1px solid ${s.type === "PEPPOL" ? C.borderAmber : C.border}`, borderRadius: 3, padding: "2px 5px", width: 42, textAlign: "center", flexShrink: 0 }}>{s.type}</span>
                      <span style={{ fontSize: 10.5, color: C.text, flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{s.name}</span>
                      <span style={{ fontSize: 9, fontFamily: num, color: C.textFaint, flexShrink: 0 }}>{s.date}</span>
                    </div>
                  ))}
                  {dd.sources.length > 1 && (
                    <div style={{ fontSize: 9, color: C.green, marginTop: 4, display: "flex", alignItems: "center", gap: 4 }}>
                      <Dot color={C.green} size={4} /> Sources matched
                    </div>
                  )}
                </div>

                {/* Transactions */}
                <div style={{ padding: "10px 0" }}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 6 }}>
                    <span style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em" }}>Transactions</span>
                    {!isReview && dd.transactions.length === 0 && (
                      <span style={{ fontSize: 10, fontWeight: 600, color: C.amber, cursor: "pointer" }}>+ Record payment</span>
                    )}
                  </div>
                  {dd.transactions.length === 0 ? (
                    <div style={{ padding: "6px 0", fontSize: 10, color: C.textFaint, textAlign: "center" }}>
                      {isReview ? "Confirm first to track payment" : "No payment recorded yet"}
                    </div>
                  ) : dd.transactions.map((t, ti) => (
                    <div key={ti} style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0" }}>
                      <Dot color={C.green} size={5} />
                      <div style={{ flex: 1 }}>
                        <div style={{ fontSize: 10.5, color: C.text }}>{t.method}</div>
                        <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{t.ref}</div>
                      </div>
                      <div style={{ textAlign: "right" }}>
                        <div style={{ fontSize: 10.5, fontFamily: num, fontWeight: 600, color: C.green }}>Paid</div>
                        <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{t.date}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Bottom actions */}
              <div style={{ padding: "10px 14px", borderTop: `1px solid ${C.border}`, flexShrink: 0 }}>
                {isReview ? (
                  <>
                    <button style={{ width: "100%", padding: "9px 0", fontSize: 12, fontWeight: 600, fontFamily: ui, color: "#fff", background: C.amber, border: "none", borderRadius: 7, cursor: "pointer", boxShadow: "0 2px 8px rgba(184,134,11,0.25)", marginBottom: 6 }}>Confirm</button>
                    <div style={{ display: "flex", gap: 6 }}>
                      <button style={{ flex: 1, padding: "6px 0", fontSize: 10, fontWeight: 500, fontFamily: ui, color: C.textMuted, background: "transparent", border: `1px solid ${C.border}`, borderRadius: 6, cursor: "pointer" }}>Flag issue</button>
                      <button style={{ flex: 1, padding: "6px 0", fontSize: 10, fontWeight: 500, fontFamily: ui, color: C.red, background: "transparent", border: `1px solid ${C.border}`, borderRadius: 6, cursor: "pointer" }}>Reject</button>
                    </div>
                  </>
                ) : (
                  <div style={{ textAlign: "center", padding: "2px 0" }}>
                    <span style={{ fontSize: 10, color: C.textFaint, cursor: "pointer", textDecoration: "underline", textDecorationColor: C.textFaint, textUnderlineOffset: 2 }}>Request amendment</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}


function DocumentsScreen({ onOpenDetail }) {
  const [tab,setTab]=useState("all");
  const filtered=tab==="attn"?ALL_DOCS.filter(d=>d.st==="warn"):tab==="ok"?ALL_DOCS.filter(d=>d.st==="ok"):ALL_DOCS;
  const cols="minmax(130px,1fr) minmax(90px,150px) 90px 70px 64px";
  return (
    <div style={{display:"flex",flexDirection:"column",gap:14}}>
      <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",flexWrap:"wrap",gap:8}}>
        <Tabs tabs={[{id:"all",label:"All",count:ALL_DOCS.length},{id:"attn",label:"Attention",count:1,countColor:C.amber,countBg:C.amberSoft},{id:"ok",label:"Confirmed",count:ALL_DOCS.filter(d=>d.st==="ok").length}]} active={tab} onChange={setTab}/>
        <div style={{display:"flex",gap:8}}>
          <div style={{display:"flex",alignItems:"center",gap:8,padding:"6px 12px",background:C.page,border:`1px solid ${C.border}`,borderRadius:7,boxShadow:C.shadow,width:160}}><span style={{fontSize:12,color:C.textFaint}}>⌕</span><span style={{fontSize:11,color:C.textMuted}}>Search…</span></div>
          <button style={{fontSize:12,fontWeight:600,fontFamily:ui,color:"#fff",background:C.amber,border:"none",borderRadius:7,padding:"7px 18px",cursor:"pointer"}}>Upload</button>
        </div>
      </div>
      <Card>
        <TH cols={cols} headers={[{label:"Vendor"},{label:"Reference"},{label:"Amount",align:"right"},{label:"Date"},{label:"Source"}]}/>
        {filtered.map((d,i,a)=>{ const origIdx = ALL_DOCS.indexOf(d); return <div key={i} onClick={()=>onOpenDetail&&onOpenDetail(origIdx)} style={{display:"grid",gridTemplateColumns:cols,padding:"0 22px",borderBottom:i<a.length-1?`1px solid ${C.border}`:"none",cursor:"pointer",transition:"background 0.15s"}} onMouseEnter={e=>e.currentTarget.style.background=C.warm} onMouseLeave={e=>e.currentTarget.style.background="transparent"}><div style={{padding:"11px 0",display:"flex",alignItems:"center",gap:8}}><Dot color={d.st==="warn"?C.amber:C.green} size={5}/><span style={{fontSize:12.5,fontWeight:500,color:C.text}}>{d.v}</span></div><div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:10.5,fontFamily:num,color:C.textMuted,overflow:"hidden",textOverflow:"ellipsis",whiteSpace:"nowrap"}}>{d.ref}</span></div><div style={{padding:"11px 0",display:"flex",alignItems:"center",justifyContent:"flex-end"}}><Amt value={d.amt} size={12}/></div><div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:11,color:C.textSec}}>{d.date}</span></div><div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:9,fontWeight:600,fontFamily:num,color:d.src==="PEPPOL"?C.amber:C.textMuted,background:d.src==="PEPPOL"?C.amberWhisper:C.canvas,border:`1px solid ${d.src==="PEPPOL"?C.borderAmber:C.border}`,borderRadius:3,padding:"2px 6px"}}>{d.src}</span></div></div>; })}
      </Card>
    </div>
  );
}


// ═══════════════════════════════════════
//  CASHFLOW
// ═══════════════════════════════════════
function CashflowScreen() {
  const [period,setPeriod]=useState("overdue");
  const [dir,setDir]=useState("all");
  const overdue=[{v:"KBC Bank",amt:-45,date:"Dec 31",days:49,desc:"Bank fees",contact:"KBC Bank NV"},{v:"Coolblue België",amt:-249.01,date:"Jan 11",days:38,desc:"Office equipment",contact:"Coolblue België NV"},{v:"Tesla Belgium",amt:-346.97,date:"Jan 14",days:35,desc:"Charging subscription",contact:"Tesla Belgium BVBA"},{v:"SRL Accounting & Tax",amt:-798.6,date:"Jan 16",days:33,desc:"Accounting Q4",contact:"SRL Accounting & Tax"},{v:"Tesla Belgium",amt:-9.99,date:"Jan 28",days:21,desc:"Premium connectivity",contact:"Tesla Belgium BVBA"},{v:"Donckers Schoten",amt:-1306.12,date:"Jan 30",days:19,desc:"Fuel expenses",contact:"Donckers Schoten NV"},{v:"KBC Bank",amt:-962.52,date:"Feb 4",days:14,desc:"Business loan",contact:"KBC Bank NV"},{v:"KBC Bank",amt:-289,date:"Feb 14",days:4,desc:"Insurance premium",contact:"KBC Bank NV"}];
  const upcoming=[{v:"KBC Bank",amt:-9.64,date:"Feb 20",days:-3,desc:"Transaction fees",contact:"KBC Bank NV"}];
  const total=overdue.reduce((s,i)=>s+Math.abs(i.amt),0);
  const items=period==="overdue"?overdue:period==="upcoming"?upcoming:[];
  const cols="80px 1fr minmax(100px,140px) 70px 90px";

  return (
    <div style={{display:"flex",flexDirection:"column",gap:14}}>
      <Card style={{padding:"24px 24px 20px"}} accent>
        <div style={{display:"flex",justifyContent:"space-between",alignItems:"flex-start"}}>
          <div>
            <Label color={C.textMuted}>Cash position</Label>
            <div style={{fontSize:32,fontWeight:700,fontFamily:num,color:C.red,letterSpacing:"-0.04em",lineHeight:1,marginTop:10}}>−€{total.toLocaleString("de-DE",{minimumFractionDigits:2})}</div>
            <div style={{fontSize:12,color:C.textMuted,marginTop:10}}>Next 30 days · In €0,00 · Out €{total.toLocaleString("de-DE",{minimumFractionDigits:2})}</div>
          </div>
          <div style={{display:"flex",flexDirection:"column",alignItems:"flex-end",gap:6}}>
            <Label>8 weeks</Label>
            <SparkBars data={[45,249,347,799,10,1306,963,289]} height={44}/>
          </div>
        </div>
      </Card>

      <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
        <Tabs tabs={[{id:"upcoming",label:"Upcoming",count:upcoming.length},{id:"overdue",label:"Overdue",count:overdue.length,countColor:C.red,countBg:C.redSoft},{id:"history",label:"History"}]} active={period} onChange={setPeriod}/>
        <div style={{display:"flex",alignItems:"center",gap:4}}>
          <Tabs tabs={[{id:"all",label:"All"},{id:"in",label:"In"},{id:"out",label:"Out"}]} active={dir} onChange={setDir}/>
          <button style={{fontSize:12,fontWeight:600,fontFamily:ui,color:C.amber,background:"transparent",border:"none",cursor:"pointer",marginLeft:12}}>Create invoice</button>
        </div>
      </div>

      <Card>
        <TH cols={cols} headers={[{label:"Due date"},{label:"Contact"},{label:"Description"},{label:"Status"},{label:"Amount",align:"right"}]}/>
        {items.length===0 ? (
          <div style={{padding:"40px 20px",textAlign:"center"}}>
            <div style={{fontSize:13,color:C.amber}}>No upcoming cash movements in the next 30 days.</div>
            <div style={{fontSize:12,color:C.textMuted,marginTop:6}}>Confirmed documents with due dates appear here.</div>
          </div>
        ) : items.map((item,i,a)=>(
          <div key={i} style={{display:"grid",gridTemplateColumns:cols,padding:"0 22px",borderBottom:i<a.length-1?`1px solid ${C.border}`:"none",cursor:"pointer",transition:"background 0.15s"}} onMouseEnter={e=>e.currentTarget.style.background=C.warm} onMouseLeave={e=>e.currentTarget.style.background="transparent"}>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:11,fontFamily:num,color:C.textSec}}>{item.date}</span></div>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:12.5,fontWeight:500,color:C.text}}>{item.contact}</span></div>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:11,color:C.textMuted}}>{item.desc}</span></div>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center"}}><span style={{fontSize:9,fontWeight:600,fontFamily:num,color:item.days>30?C.red:item.days>14?C.amber:C.textSec,background:item.days>30?C.redSoft:item.days>14?C.amberSoft:C.canvas,borderRadius:3,padding:"2px 7px"}}>{item.days>0?`${item.days}d late`:`in ${Math.abs(item.days)}d`}</span></div>
            <div style={{padding:"11px 0",display:"flex",alignItems:"center",justifyContent:"flex-end"}}><Amt value={item.amt} size={12}/></div>
          </div>
        ))}
      </Card>
    </div>
  );
}


// ═══════════════════════════════════════
//  COMPANY DETAILS
// ═══════════════════════════════════════
function CompanyDetailsScreen() {
  const peppolRows = [
    { label: "Participant ID", value: "0208:BE0777887045", status: "Verified", statusColor: C.green },
    { label: "Access Point", value: "Managed by Dokus", status: "Connected", statusColor: C.green },
    { label: "Inbound", value: "Active", status: "2d ago", statusColor: C.amber },
    { label: "Outbound", value: "Active", status: "Active", statusColor: C.green },
  ];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 0, maxWidth: 640 }}>
      {/* PEPPOL Connection — expanded by default */}
      <Card style={{ padding: "20px 24px", marginBottom: 20 }} accent>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 16 }}>
          <span style={{ fontSize: 14, fontWeight: 700, color: C.text }}>PEPPOL Connection</span>
          <StatusBadge label="Compliant" color={C.green} />
        </div>
        {peppolRows.map((row, i) => (
          <div key={i} style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "10px 0", borderTop: i === 0 ? `1px solid ${C.border}` : "none", borderBottom: `1px solid ${C.border}` }}>
            <span style={{ fontSize: 12, color: C.textMuted, width: 120, flexShrink: 0 }}>{row.label}</span>
            <span style={{ fontSize: 12.5, fontWeight: 500, fontFamily: row.label === "Participant ID" ? num : ui, color: C.text, flex: 1 }}>{row.value}</span>
            <StatusBadge label={row.status} color={row.statusColor} />
          </div>
        ))}
        <div style={{ marginTop: 14, fontSize: 12, color: C.textMuted, lineHeight: 1.5 }}>
          Belgium PEPPOL mandate effective January 1, 2026.<br />Your business is compliant.
        </div>
      </Card>

      <div style={{ padding: "0" }}>
        <Collapsible title="Legal Identity" right="INVOID VISION">
          <div style={{ display: "flex", flexDirection: "column", gap: 10, paddingLeft: 18 }}>
            {[{l:"Company Name",v:"INVOID VISION"},{l:"VAT Number",v:"BE0777.887.045"},{l:"Legal Form",v:"Besloten vennootschap (BV)"},{l:"Registered Address",v:"Belgium"}].map((r,i) => (
              <div key={i} style={{ display: "flex", gap: 16 }}>
                <span style={{ fontSize: 12, color: C.textMuted, width: 130, flexShrink: 0 }}>{r.l}</span>
                <span style={{ fontSize: 12.5, fontWeight: 500, color: C.text }}>{r.v}</span>
              </div>
            ))}
          </div>
        </Collapsible>

        <Collapsible title="Banking Details">
          <div style={{ paddingLeft: 18, fontSize: 12, color: C.textMuted }}>Connect your bank account to enable payment tracking and reconciliation.</div>
        </Collapsible>

        <Collapsible title="Invoice Settings" right="INV-2026-0001">
          <div style={{ display: "flex", flexDirection: "column", gap: 10, paddingLeft: 18 }}>
            {[{l:"Prefix",v:"INV"},{l:"Next Number",v:"2026-0001"},{l:"Default Currency",v:"EUR"},{l:"Default VAT",v:"21%"}].map((r,i) => (
              <div key={i} style={{ display: "flex", gap: 16 }}>
                <span style={{ fontSize: 12, color: C.textMuted, width: 130, flexShrink: 0 }}>{r.l}</span>
                <span style={{ fontSize: 12.5, fontWeight: 500, fontFamily: num, color: C.text }}>{r.v}</span>
              </div>
            ))}
          </div>
        </Collapsible>

        <Collapsible title="Payment Terms" right="30 days">
          <div style={{ display: "flex", flexDirection: "column", gap: 10, paddingLeft: 18 }}>
            {[{l:"Default Terms",v:"30 days"},{l:"Late Fee",v:"Not configured"},{l:"Early Payment",v:"Not configured"}].map((r,i) => (
              <div key={i} style={{ display: "flex", gap: 16 }}>
                <span style={{ fontSize: 12, color: C.textMuted, width: 130, flexShrink: 0 }}>{r.l}</span>
                <span style={{ fontSize: 12.5, fontWeight: 500, color: i > 0 ? C.textMuted : C.text }}>{r.v}</span>
              </div>
            ))}
          </div>
        </Collapsible>
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  CONTACTS — Master-Detail
// ═══════════════════════════════════════
function ContactsScreen() {
  const contacts = [
    { name: "Coolblue België NV", initials: "CB", vat: "BE0867686774", invoices: 2, total: 538.01, terms: "30 days", role: "Vendor", docs: [
      { ref: "BE0867686774", amt: -249.01, date: "Jan 11", type: "Invoice" },
      { ref: "peppol-7ff798f8", amt: -289.00, date: "Feb 11", type: "Invoice" },
    ]},
    { name: "Donckers Schoten NV", initials: "DS", vat: "BE0428927169", invoices: 1, total: 1306.12, terms: "30 days", role: "Vendor", docs: [
      { ref: "100111009120", amt: -1306.12, date: "Jan 30", type: "Invoice" },
    ]},
    { name: "KBC Bank NV", initials: "KB", vat: "BE0462920226", invoices: 4, total: 1296.52, terms: "30 days", role: "Bank", docs: [
      { ref: "384421507", amt: -289.00, date: "Feb 14", type: "Invoice" },
      { ref: "00010001BE26", amt: -962.52, date: "Jan 21", type: "Invoice" },
      { ref: "2504773248", amt: -45.00, date: "Dec 31", type: "Invoice" },
    ]},
    { name: "SRL Accounting & Tax", initials: "SA", vat: "BE0123456789", invoices: 1, total: 798.60, terms: "30 days", role: "Accountant", docs: [
      { ref: "INVOID-2026-01", amt: -798.60, date: "Jan 2", type: "Invoice" },
    ]},
    { name: "Studiebureel v. Automobiel", initials: "SV", vat: "BE0404567890", invoices: 0, total: 0, terms: "30 days", role: "Vendor", docs: [] },
    { name: "Tesla Belgium BVBA", initials: "TB", vat: "BE0554789012", invoices: 2, total: 356.96, terms: "30 days", role: "Vendor", docs: [
      { ref: "peppol-439380c9", amt: -9.99, date: "Jan 28", type: "Invoice" },
      { ref: "peppol-71b40a13", amt: -346.97, date: "Jan 14", type: "Invoice" },
    ]},
  ];
  const [selected, setSelected] = useState(0);
  const c = contacts[selected];

  const roleColor = (r) => r === "Bank" ? "#5b7bb4" : r === "Accountant" ? C.amber : C.textMuted;

  return (
    <div style={{ display: "grid", gridTemplateColumns: "240px 1fr", gap: 0, minHeight: 460 }}>
      {/* ── Contact List ── */}
      <div style={{ borderRight: `1px solid ${C.border}`, display: "flex", flexDirection: "column" }}>
        <div style={{ padding: "0 12px 12px", display: "flex", gap: 8 }}>
          <div style={{ flex: 1, display: "flex", alignItems: "center", gap: 8, padding: "7px 10px", background: C.canvas, borderRadius: 8, border: `1px solid ${C.border}` }}>
            <span style={{ fontSize: 12, color: C.textFaint }}>⌕</span>
            <span style={{ fontSize: 11, color: C.textMuted }}>Search…</span>
          </div>
          <button style={{ width: 30, height: 30, borderRadius: 8, border: `1px solid ${C.borderAmber}`, background: C.amberWhisper, color: C.amber, fontSize: 15, fontWeight: 400, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>+</button>
        </div>

        <div style={{ flex: 1, overflow: "auto" }}>
          {contacts.map((ct, i) => (
            <div key={i} onClick={() => setSelected(i)} style={{
              padding: "10px 12px", display: "flex", alignItems: "center", gap: 10,
              cursor: "pointer", transition: "all 0.12s",
              background: selected === i ? C.warm : "transparent",
              borderRight: selected === i ? `2px solid ${C.amber}` : "2px solid transparent",
            }}
              onMouseEnter={e => { if (i !== selected) e.currentTarget.style.background = "rgba(0,0,0,0.015)"; }}
              onMouseLeave={e => { if (i !== selected) e.currentTarget.style.background = "transparent"; }}
            >
              <div style={{
                width: 32, height: 32, borderRadius: 8, flexShrink: 0,
                background: selected === i ? C.amberSoft : C.canvas,
                border: `1px solid ${selected === i ? C.borderAmber : C.border}`,
                display: "flex", alignItems: "center", justifyContent: "center",
                fontSize: 10, fontWeight: 700, fontFamily: num,
                color: selected === i ? C.amber : C.textMuted,
              }}>{ct.initials}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 12, fontWeight: selected === i ? 600 : 450, color: C.text, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{ct.name}</div>
                <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 2 }}>
                  <span style={{ fontSize: 9, fontWeight: 600, color: roleColor(ct.role) }}>{ct.role}</span>
                  {ct.invoices > 0 && <span style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{ct.invoices} doc{ct.invoices !== 1 ? "s" : ""}</span>}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ── Contact Detail ── */}
      <div style={{ overflow: "auto", padding: "0 28px 28px" }}>
        {/* Hero header */}
        <div style={{ display: "flex", alignItems: "center", gap: 16, paddingBottom: 20, borderBottom: `1px solid ${C.border}` }}>
          <div style={{
            width: 52, height: 52, borderRadius: 14, flexShrink: 0,
            background: `linear-gradient(135deg, ${C.amberSoft}, rgba(184,134,11,0.12))`,
            border: `1px solid ${C.borderAmber}`,
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: 16, fontWeight: 700, fontFamily: num, color: C.amber,
          }}>{c.initials}</div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 17, fontWeight: 700, color: C.text, letterSpacing: "-0.02em" }}>{c.name}</div>
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 4 }}>
              <span style={{ fontSize: 10, fontWeight: 600, color: roleColor(c.role), background: c.role === "Bank" ? "rgba(91,123,180,0.08)" : c.role === "Accountant" ? C.amberSoft : C.canvas, borderRadius: 4, padding: "2px 7px" }}>{c.role}</span>
              <span style={{ fontSize: 10, fontFamily: num, color: C.textMuted }}>{c.vat}</span>
            </div>
          </div>
          <Dot color={C.green} pulse size={5} />
        </div>

        {/* Key-value info rows */}
        <div style={{ padding: "16px 0", borderBottom: `1px solid ${C.border}` }}>
          {[
            { l: "VAT Number", v: c.vat, mono: true },
            { l: "Payment Terms", v: c.terms },
            { l: "Total Volume", v: `€${c.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}`, mono: true, color: c.total > 0 ? C.text : C.textFaint },
            { l: "Documents", v: `${c.invoices}`, mono: true, color: c.invoices > 0 ? C.text : C.textFaint },
          ].map((r, i) => (
            <div key={i} style={{ display: "flex", alignItems: "center", padding: "7px 0" }}>
              <span style={{ fontSize: 12, color: C.textMuted, width: 120, flexShrink: 0 }}>{r.l}</span>
              <span style={{ fontSize: 12.5, fontWeight: 500, fontFamily: r.mono ? num : ui, color: r.color || C.text }}>{r.v}</span>
            </div>
          ))}
        </div>

        {/* Recent Documents */}
        <div style={{ paddingTop: 16 }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 10 }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: C.text }}>Recent Documents</span>
            {c.docs.length > 0 && <span style={{ fontSize: 11, color: C.textMuted, cursor: "pointer" }}>View all →</span>}
          </div>
          {c.docs.length === 0 ? (
            <div style={{ padding: "24px 0", textAlign: "center", fontSize: 12, color: C.textMuted }}>No documents yet</div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 1 }}>
              {c.docs.map((d, i) => (
                <div key={i} style={{
                  display: "flex", alignItems: "center", gap: 12, padding: "9px 10px",
                  borderRadius: 8, cursor: "pointer", transition: "background 0.12s",
                }}
                  onMouseEnter={e => e.currentTarget.style.background = C.warm}
                  onMouseLeave={e => e.currentTarget.style.background = "transparent"}
                >
                  <div style={{ width: 4, height: 4, borderRadius: "50%", background: C.amber, flexShrink: 0 }} />
                  <span style={{ fontSize: 11, fontFamily: num, color: C.textMuted, width: 50, flexShrink: 0 }}>{d.date}</span>
                  <span style={{ fontSize: 12, color: C.text, flex: 1 }}>{d.type}</span>
                  <span style={{ fontSize: 10, fontFamily: num, color: C.textMuted, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", maxWidth: 110 }}>{d.ref}</span>
                  <Amt value={d.amt} size={11} weight={500} />
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Notes */}
        <div style={{ marginTop: 16, paddingTop: 16, borderTop: `1px solid ${C.border}` }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: C.text }}>Notes</span>
            <span style={{ fontSize: 12, fontWeight: 500, color: C.amber, cursor: "pointer" }}>+ Add</span>
          </div>
          <div style={{ padding: "20px 0 8px", textAlign: "center", fontSize: 12, color: C.textFaint }}>No notes</div>
        </div>
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  ACCOUNTANT
// ═══════════════════════════════════════
function AccountantScreen() {
  return (
    <div style={{display:"flex",flexDirection:"column",gap:20,maxWidth:560}}>
      <Card style={{padding:"24px 24px 20px"}} accent>
        <div style={{display:"flex",alignItems:"flex-start",gap:14}}><div style={{width:40,height:40,borderRadius:9,background:C.amberSoft,border:`1px solid ${C.borderAmber}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:18,flexShrink:0}}>↓</div><div><div style={{fontSize:17,fontWeight:700,color:C.text}}>Q1 2026</div><div style={{fontSize:12,color:C.textMuted,marginTop:3}}>January – March 2026</div></div></div>
        <div style={{display:"flex",flexDirection:"column",gap:12,marginTop:20}}>
          {[{label:"Invoices received",count:10,ready:true},{label:"Credit notes",count:0,ready:true},{label:"Payment records",count:0,ready:false,sub:"Connect bank to enable"}].map((item,i)=><div key={i} style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}><div style={{display:"flex",alignItems:"center",gap:9}}><Dot color={item.ready?C.green:C.textFaint} size={5}/><div><span style={{fontSize:12.5,color:C.text,fontWeight:500}}>{item.label}</span>{item.sub&&<div style={{fontSize:10.5,color:C.textMuted,marginTop:1}}>{item.sub}</div>}</div></div><span style={{fontSize:12,fontWeight:600,fontFamily:num,color:item.count>0?C.text:C.textFaint}}>{item.count}</span></div>)}
        </div>
        <div style={{marginTop:20,paddingTop:18,borderTop:`1px solid ${C.border}`}}>
          <button style={{width:"100%",fontSize:13,fontWeight:600,fontFamily:ui,color:"#fff",background:C.amber,border:"none",borderRadius:7,padding:"11px 0",cursor:"pointer",boxShadow:"0 2px 8px rgba(184,134,11,0.25)"}}>Prepare export</button>
          <div style={{fontSize:10.5,color:C.textMuted,textAlign:"center",marginTop:8}}>Generates a structured package for your accountant</div>
        </div>
      </Card>
      <div><SectionTitle>Previous periods</SectionTitle><Card>{["Q4 2025","Q3 2025"].map((p,i,a)=><div key={i} style={{padding:"12px 18px",display:"flex",alignItems:"center",justifyContent:"space-between",borderBottom:i<a.length-1?`1px solid ${C.border}`:"none"}}><span style={{fontSize:12.5,fontWeight:600,color:C.text}}>{p}</span><span style={{fontSize:10.5,fontFamily:num,color:C.textFaint}}>No data</span></div>)}</Card></div>
      <div><SectionTitle>Your accountant</SectionTitle><Card style={{padding:"16px 18px",cursor:"pointer"}}><div style={{display:"flex",alignItems:"center",gap:12}}><div style={{width:32,height:32,borderRadius:"50%",background:C.canvas,border:`1px solid ${C.border}`,display:"flex",alignItems:"center",justifyContent:"center",fontSize:12,fontWeight:600,color:C.textMuted}}>+</div><div><div style={{fontSize:12.5,fontWeight:500,color:C.textSec}}>Connect your accountant</div><div style={{fontSize:10.5,color:C.textMuted,marginTop:1}}>Give read-only access to exports</div></div></div></Card></div>
    </div>
  );
}


function TeamScreen() {
  return (
    <div style={{ maxWidth: 400, margin: "0 auto", paddingTop: 12 }}>
      {/* You */}
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", textAlign: "center", marginBottom: 32 }}>
        <div style={{
          width: 64, height: 64, borderRadius: 20, marginBottom: 12,
          background: `linear-gradient(135deg, ${C.amberSoft}, rgba(184,134,11,0.15))`,
          border: `1.5px solid ${C.borderAmber}`,
          display: "flex", alignItems: "center", justifyContent: "center",
          fontSize: 20, fontWeight: 700, fontFamily: num, color: C.amber,
        }}>AK</div>
        <div style={{ fontSize: 17, fontWeight: 700, color: C.text, letterSpacing: "-0.02em" }}>Artem Kuznetsov</div>
        <div style={{ fontSize: 12, color: C.textMuted, marginTop: 3 }}>artem@invoid.vision</div>
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 8 }}>
          <span style={{ fontSize: 10, fontWeight: 600, fontFamily: num, color: C.amber, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, borderRadius: 5, padding: "3px 10px" }}>Owner</span>
          <span style={{ fontSize: 10, fontFamily: num, color: C.textFaint }}>since Feb 15, 2026</span>
        </div>
      </div>

      {/* Team list — just rows, like iOS settings */}
      <div style={{ background: C.page, borderRadius: 12, border: `1px solid ${C.border}`, boxShadow: C.shadow, overflow: "hidden" }}>
        {/* You — the only member */}
        <div style={{ display: "flex", alignItems: "center", gap: 12, padding: "14px 18px", borderBottom: `1px solid ${C.border}` }}>
          <div style={{
            width: 34, height: 34, borderRadius: 10, flexShrink: 0,
            background: C.amberSoft, border: `1px solid ${C.borderAmber}`,
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: 11, fontWeight: 700, fontFamily: num, color: C.amber,
          }}>AK</div>
          <div style={{ flex: 1 }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: C.text }}>Artem Kuznetsov</span>
            <span style={{ fontSize: 10, color: C.textMuted, marginLeft: 6 }}>You</span>
          </div>
          <Dot color={C.green} pulse size={5} />
        </div>

        {/* Invite row — like adding a contact */}
        <div style={{
          display: "flex", alignItems: "center", gap: 12, padding: "14px 18px",
          cursor: "pointer", transition: "background 0.12s",
        }}
          onMouseEnter={e => e.currentTarget.style.background = C.warm}
          onMouseLeave={e => e.currentTarget.style.background = "transparent"}
        >
          <div style={{
            width: 34, height: 34, borderRadius: 10, flexShrink: 0,
            background: C.canvas, border: `1px dashed ${C.textFaint}`,
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: 16, color: C.textMuted,
          }}>+</div>
          <div style={{ flex: 1 }}>
            <span style={{ fontSize: 13, fontWeight: 500, color: C.amber }}>Invite member</span>
            <div style={{ fontSize: 10, color: C.textFaint, marginTop: 1 }}>2 seats available</div>
          </div>
          <span style={{ fontSize: 14, color: C.textFaint }}>›</span>
        </div>
      </div>

      {/* Quiet footer note */}
      <div style={{ textAlign: "center", marginTop: 20, fontSize: 11, color: C.textFaint, lineHeight: 1.5 }}>
        Accountants get read-only export access.<br />
        Core includes 3 seats.
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  PROFILE
// ═══════════════════════════════════════
function ProfileScreen() {
  // Settings row helper
  const Row = ({ label, value, mono, action, chevron, destructive, last }) => (
    <div style={{
      display: "flex", alignItems: "center", padding: "12px 18px",
      borderBottom: last ? "none" : `1px solid ${C.border}`,
      cursor: action || chevron ? "pointer" : "default",
      transition: "background 0.12s",
    }}
      onMouseEnter={e => { if (action || chevron) e.currentTarget.style.background = C.warm; }}
      onMouseLeave={e => e.currentTarget.style.background = "transparent"}
    >
      <span style={{ fontSize: 12.5, color: destructive ? C.red : C.text, fontWeight: destructive ? 500 : 400, flex: 1 }}>{label}</span>
      {value && <span style={{ fontSize: 12, fontFamily: mono ? num : ui, color: C.textMuted }}>{value}</span>}
      {chevron && <span style={{ fontSize: 13, color: C.textFaint, marginLeft: 8 }}>›</span>}
    </div>
  );

  return (
    <div style={{ maxWidth: 440, margin: "0 auto", paddingTop: 8 }}>
      {/* Avatar + identity */}
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", textAlign: "center", marginBottom: 28 }}>
        <div style={{
          width: 72, height: 72, borderRadius: 22, marginBottom: 14,
          background: `linear-gradient(135deg, ${C.amberSoft}, rgba(184,134,11,0.15))`,
          border: `1.5px solid ${C.borderAmber}`,
          display: "flex", alignItems: "center", justifyContent: "center",
          fontSize: 22, fontWeight: 700, fontFamily: num, color: C.amber,
        }}>AK</div>
        <div style={{ fontSize: 18, fontWeight: 700, color: C.text, letterSpacing: "-0.02em" }}>Artem Kuznetsov</div>
        <div style={{ fontSize: 12, color: C.textMuted, marginTop: 3 }}>artem@invoid.vision</div>
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 10 }}>
          <span style={{ fontSize: 10, fontWeight: 600, fontFamily: num, color: C.amber, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, borderRadius: 5, padding: "3px 10px" }}>Core</span>
          <span style={{ fontSize: 10, fontWeight: 600, fontFamily: num, color: C.amber, background: C.amberWhisper, border: `1px solid ${C.borderAmber}`, borderRadius: 5, padding: "3px 10px" }}>Owner</span>
        </div>
      </div>

      {/* Account */}
      <Card style={{ marginBottom: 14, overflow: "hidden" }}>
        <Row label="Email" value="artem@invoid.vision" mono />
        <Row label="Name" value="Artem Kuznetsov" />
        <Row label="Email Verification" value={
          <span style={{ display: "flex", alignItems: "center", gap: 5 }}>
            <Dot color={C.amber} size={5} />
            <span style={{ color: C.amber, fontSize: 11, fontWeight: 500 }}>Not verified</span>
          </span>
        } />
        <Row label="Verify email" chevron last />
      </Card>

      {/* Security */}
      <Card style={{ marginBottom: 14, overflow: "hidden" }}>
        <Row label="Change Password" chevron />
        <Row label="Active Sessions" value="1 device" chevron last />
      </Card>

      {/* Server */}
      <Card style={{ marginBottom: 14, overflow: "hidden" }}>
        <div style={{ padding: "12px 18px 6px", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Label>Server</Label>
          <Dot color={C.green} pulse size={5} />
        </div>
        <Row label="Server" value="Dokus Server" />
        <Row label="URL" value="192.168.0.193:8000" mono />
        <Row label="Version" value="0.1.0" mono />
        <Row label="Change Server" chevron />
        <div style={{ padding: "8px 18px 12px", display: "flex", justifyContent: "center" }}>
          <span style={{ fontSize: 11, color: C.amber, fontWeight: 500, cursor: "pointer" }}>☁ Reset to Cloud</span>
        </div>
      </Card>

      {/* Danger */}
      <Card style={{ marginBottom: 14, overflow: "hidden" }}>
        <div style={{ padding: "12px 18px 6px" }}><Label color={C.red}>Danger Zone</Label></div>
        <div style={{ padding: "0 18px 8px" }}><span style={{ fontSize: 11, color: C.textMuted }}>Deactivating removes access to all workspaces.</span></div>
        <Row label="Deactivate Account" destructive last />
      </Card>

      {/* Log out */}
      <Card style={{ overflow: "hidden" }}>
        <Row label="Log Out" destructive last />
      </Card>

      {/* Version footer */}
      <div style={{ textAlign: "center", marginTop: 18, paddingBottom: 8, fontSize: 10, fontFamily: num, color: C.textFaint }}>
        Dokus v0.1.0 · Core
      </div>
    </div>
  );
}



function ContentWindow({ screen, children }) {
  const [displayed, setDisplayed] = useState({ key: screen, content: children });
  const [animState, setAnimState] = useState("idle");
  const prevScreen = useRef(screen);
  useEffect(() => {
    if (screen !== prevScreen.current) {
      prevScreen.current = screen;
      setAnimState("exit");
      setTimeout(() => { setDisplayed({ key: screen, content: children }); setAnimState("enter"); setTimeout(() => setAnimState("idle"), 320); }, 220);
    } else { setDisplayed({ key: screen, content: children }); }
  }, [screen, children]);
  const meta = screenMeta[screen] || { title: screen, sub: "" };
  return (
    <div style={{ flex:1, background:C.glassContent, backdropFilter:"blur(60px)",WebkitBackdropFilter:"blur(60px)", border:`1px solid ${C.glassBorder}`,borderRadius:16, boxShadow:`0 8px 32px rgba(0,0,0,0.06),0 1px 2px rgba(0,0,0,0.03),inset 0 1px 0 rgba(255,255,255,0.5)`, display:"flex",flexDirection:"column",overflow:"hidden",position:"relative" }}>
      <div style={{ padding:"14px 24px 12px",borderBottom:`1px solid rgba(0,0,0,0.05)`,background:C.glassHeader,backdropFilter:"blur(20px)",WebkitBackdropFilter:"blur(20px)",display:"flex",alignItems:"center",justifyContent:"space-between",flexShrink:0 }}>
        <div><div style={{fontSize:15,fontWeight:700,color:C.text,letterSpacing:"-0.02em"}}>{meta.title}</div><div style={{fontSize:11,color:C.textMuted,marginTop:1}}>{meta.sub}</div></div>
        <span style={{fontSize:9,fontFamily:num,color:C.textFaint}}>{new Date().toLocaleDateString("en-GB",{day:"numeric",month:"short",year:"numeric"})}</span>
      </div>
      <div style={{flex:1,overflow:"auto",position:"relative"}}>
        <div key={displayed.key} style={{ padding: ["contacts"].includes(displayed.key) ? "0" : "24px 28px", height: ["contacts"].includes(displayed.key) ? "100%" : "auto", animation:animState==="exit"?"frame-exit 0.22s ease forwards":animState==="enter"?"frame-enter 0.3s ease both":"none" }}>{displayed.content}</div>
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  DESKTOP SHELL
// ═══════════════════════════════════════
function DesktopShell({ screen, setScreen }) {
  const [profileOpen, setProfileOpen] = useState(false);
  const [docDetailIdx, setDocDetailIdx] = useState(null);

  // Document detail is a full-screen takeover — own windows, no sidebar
  if (docDetailIdx !== null) {
    return <DocumentDetailMode docs={ALL_DOCS} initialIdx={docDetailIdx} onExit={() => setDocDetailIdx(null)} />;
  }

  const content = (() => {
    switch (screen) {
      case "today": return <TodayScreen />;
      case "documents": return <DocumentsScreen onOpenDetail={(idx) => setDocDetailIdx(idx)} />;
      case "cashflow": return <CashflowScreen />;
      case "accountant": return <AccountantScreen />;
      case "company-details": return <CompanyDetailsScreen />;
      case "contacts": return <ContactsScreen />;
      case "team": return <TeamScreen />;
      case "profile": return <ProfileScreen />;
      case "ai-chat": return <div style={{display:"flex",flexDirection:"column",alignItems:"center",justifyContent:"center",padding:"60px 20px",textAlign:"center"}}><div style={{fontSize:15,fontWeight:600,color:C.text}}>AI Chat</div><div style={{fontSize:12,color:C.textMuted,marginTop:6,maxWidth:300}}>Intelligence layer for document analysis and financial insights. Coming in One tier.</div></div>;
      default: return <TodayScreen />;
    }
  })();
  return (
    <div style={{display:"flex",height:"100%",background:C.bg,fontFamily:ui,color:C.text,position:"relative"}}>
      <AmbientBackground />
      <div style={{display:"flex",flex:1,gap:10,padding:10,position:"relative",zIndex:1}}>
        <div style={{ position: "relative", display: "flex", flexShrink: 0 }}>
          <FloatingSidebar screen={screen} setScreen={setScreen} onProfileClick={() => setProfileOpen(!profileOpen)} />
          {profileOpen && <ProfilePopover onClose={() => setProfileOpen(false)} onNavigate={setScreen} />}
        </div>
        <ContentWindow screen={screen}>{content}</ContentWindow>
      </div>
    </div>
  );
}


// ═══════════════════════════════════════
//  MOBILE CONTACTS — list → detail drill-down
// ═══════════════════════════════════════
function MobileContactsScreen() {
  const contacts = [
    { name: "Coolblue België NV", initials: "CB", vat: "BE0867686774", invoices: 2, total: 538.01, terms: "30 days", role: "Vendor", docs: [
      { ref: "BE0867686774", amt: -249.01, date: "Jan 11", type: "Invoice" },
      { ref: "peppol-7ff798f8", amt: -289.00, date: "Feb 11", type: "Invoice" },
    ]},
    { name: "Donckers Schoten NV", initials: "DS", vat: "BE0428927169", invoices: 1, total: 1306.12, terms: "30 days", role: "Vendor", docs: [
      { ref: "100111009120", amt: -1306.12, date: "Jan 30", type: "Invoice" },
    ]},
    { name: "KBC Bank NV", initials: "KB", vat: "BE0462920226", invoices: 4, total: 1296.52, terms: "30 days", role: "Bank", docs: [
      { ref: "384421507", amt: -289.00, date: "Feb 14", type: "Invoice" },
      { ref: "00010001BE26", amt: -962.52, date: "Jan 21", type: "Invoice" },
      { ref: "2504773248", amt: -45.00, date: "Dec 31", type: "Invoice" },
    ]},
    { name: "SRL Accounting & Tax", initials: "SA", vat: "BE0123456789", invoices: 1, total: 798.60, terms: "30 days", role: "Accountant", docs: [
      { ref: "INVOID-2026-01", amt: -798.60, date: "Jan 2", type: "Invoice" },
    ]},
    { name: "Studiebureel v. Automobiel", initials: "SV", vat: "BE0404567890", invoices: 0, total: 0, terms: "30 days", role: "Vendor", docs: [] },
    { name: "Tesla Belgium BVBA", initials: "TB", vat: "BE0554789012", invoices: 2, total: 356.96, terms: "30 days", role: "Vendor", docs: [
      { ref: "peppol-439380c9", amt: -9.99, date: "Jan 28", type: "Invoice" },
      { ref: "peppol-71b40a13", amt: -346.97, date: "Jan 14", type: "Invoice" },
    ]},
  ];

  const [selected, setSelected] = useState(null);
  const roleColor = (r) => r === "Bank" ? "#5b7bb4" : r === "Accountant" ? C.amber : C.textMuted;

  // ── DETAIL VIEW ──
  if (selected !== null) {
    const c = contacts[selected];
    return (
      <div style={{ display: "flex", flexDirection: "column", gap: 0, animation: "rise 0.2s ease" }}>
        {/* Back button */}
        <button onClick={() => setSelected(null)} style={{
          display: "flex", alignItems: "center", gap: 4, padding: "0 0 14px",
          fontSize: 13, fontWeight: 500, color: C.amber, background: "none",
          border: "none", cursor: "pointer", fontFamily: ui,
        }}>
          <svg width="9" height="16" viewBox="0 0 9 16" fill="none" style={{ marginRight: 2 }}>
            <path d="M8 1L1.5 8L8 15" stroke={C.amber} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          Contacts
        </button>

        {/* Hero card */}
        <Card style={{ padding: "20px", marginBottom: 14 }} accent>
          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <div style={{
              width: 48, height: 48, borderRadius: 14, flexShrink: 0,
              background: `linear-gradient(135deg, ${C.amberSoft}, rgba(184,134,11,0.12))`,
              border: `1px solid ${C.borderAmber}`,
              display: "flex", alignItems: "center", justifyContent: "center",
              fontSize: 15, fontWeight: 700, fontFamily: num, color: C.amber,
            }}>{c.initials}</div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 16, fontWeight: 700, color: C.text }}>{c.name}</div>
              <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 4 }}>
                <span style={{ fontSize: 10, fontWeight: 600, color: roleColor(c.role), background: c.role === "Bank" ? "rgba(91,123,180,0.08)" : c.role === "Accountant" ? C.amberSoft : C.canvas, borderRadius: 4, padding: "2px 7px" }}>{c.role}</span>
                <Dot color={C.green} pulse size={4} />
              </div>
            </div>
          </div>
        </Card>

        {/* Info rows */}
        <Card style={{ padding: "4px 16px", marginBottom: 14 }}>
          {[
            { l: "VAT", v: c.vat, mono: true },
            { l: "Terms", v: c.terms },
            { l: "Volume", v: `€${c.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}`, mono: true },
            { l: "Documents", v: `${c.invoices}`, mono: true },
          ].map((r, i, a) => (
            <div key={i} style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "11px 0", borderBottom: i < a.length - 1 ? `1px solid ${C.border}` : "none" }}>
              <span style={{ fontSize: 12, color: C.textMuted }}>{r.l}</span>
              <span style={{ fontSize: 12.5, fontWeight: 500, fontFamily: r.mono ? num : ui, color: C.text }}>{r.v}</span>
            </div>
          ))}
        </Card>

        {/* Documents */}
        <div style={{ marginBottom: 14 }}>
          <div style={{ fontSize: 13, fontWeight: 600, color: C.text, marginBottom: 8 }}>Documents</div>
          {c.docs.length === 0 ? (
            <Card style={{ padding: "20px", textAlign: "center" }}>
              <span style={{ fontSize: 12, color: C.textMuted }}>No documents yet</span>
            </Card>
          ) : (
            <Card>
              {c.docs.map((d, i, a) => (
                <div key={i} style={{ display: "flex", alignItems: "center", gap: 10, padding: "11px 14px", borderBottom: i < a.length - 1 ? `1px solid ${C.border}` : "none" }}>
                  <div style={{ width: 4, height: 4, borderRadius: "50%", background: C.amber, flexShrink: 0 }} />
                  <span style={{ fontSize: 11, fontFamily: num, color: C.textMuted, width: 44, flexShrink: 0 }}>{d.date}</span>
                  <span style={{ fontSize: 12, color: C.text, flex: 1 }}>{d.type}</span>
                  <Amt value={d.amt} size={11} weight={500} />
                </div>
              ))}
            </Card>
          )}
        </div>

        {/* Notes */}
        <Card style={{ padding: "14px 16px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: C.text }}>Notes</span>
            <span style={{ fontSize: 12, color: C.amber }}>+ Add</span>
          </div>
          <div style={{ padding: "12px 0 4px", fontSize: 12, color: C.textFaint, textAlign: "center" }}>No notes</div>
        </Card>
      </div>
    );
  }

  // ── LIST VIEW ──
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
      {/* Search */}
      <div style={{ display: "flex", gap: 8, marginBottom: 4 }}>
        <div style={{ flex: 1, display: "flex", alignItems: "center", gap: 8, padding: "9px 12px", background: C.page, borderRadius: 10, border: `1px solid ${C.border}`, boxShadow: C.shadow }}>
          <span style={{ fontSize: 13, color: C.textFaint }}>⌕</span>
          <span style={{ fontSize: 12, color: C.textMuted }}>Search contacts…</span>
        </div>
        <button style={{ width: 36, height: 36, borderRadius: 10, border: `1px solid ${C.borderAmber}`, background: C.amberWhisper, color: C.amber, fontSize: 18, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>+</button>
      </div>

      {/* Contact cards */}
      {contacts.map((ct, i) => (
        <Card key={i} style={{ cursor: "pointer" }} accent={ct.role === "Bank" || ct.role === "Accountant"}>
          <div onClick={() => setSelected(i)} style={{ display: "flex", alignItems: "center", gap: 12, padding: "13px 14px" }}>
            <div style={{
              width: 38, height: 38, borderRadius: 10, flexShrink: 0,
              background: C.amberSoft, border: `1px solid ${C.borderAmber}`,
              display: "flex", alignItems: "center", justifyContent: "center",
              fontSize: 11, fontWeight: 700, fontFamily: num, color: C.amber,
            }}>{ct.initials}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: C.text }}>{ct.name}</div>
              <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 3 }}>
                <span style={{ fontSize: 9, fontWeight: 600, color: roleColor(ct.role) }}>{ct.role}</span>
                <span style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{ct.vat}</span>
              </div>
            </div>
            <div style={{ textAlign: "right", flexShrink: 0 }}>
              {ct.invoices > 0 ? (
                <>
                  <div style={{ fontSize: 13, fontWeight: 600, fontFamily: num, color: C.text }}>€{ct.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</div>
                  <div style={{ fontSize: 9, color: C.textMuted, marginTop: 2 }}>{ct.invoices} doc{ct.invoices !== 1 ? "s" : ""}</div>
                </>
              ) : (
                <span style={{ fontSize: 11, color: C.textFaint }}>No docs</span>
              )}
            </div>
            <span style={{ fontSize: 14, color: C.textFaint, marginLeft: 4 }}>›</span>
          </div>
        </Card>
      ))}
    </div>
  );
}


// ═══════════════════════════════════════
//  MOBILE DOCUMENTS — list → detail drill-down
// ═══════════════════════════════════════
function MobileDocumentsScreen() {
  const [selected, setSelected] = useState(null);

  // ── DETAIL VIEW ──
  if (selected !== null) {
    const doc = ALL_DOCS[selected];
    const dd = getDocData(doc);
    const isReview = doc.st === "warn";
    const initials = (dd.vendor || "??").split(" ").map(w => w[0]).join("").slice(0, 2);

    return (
      <div style={{ display: "flex", flexDirection: "column", gap: 0, animation: "rise 0.2s ease", margin: "-16px -14px -86px", minHeight: "calc(100% + 86px)" }}>
        {/* Back + status bar */}
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "0 14px 12px" }}>
          <button onClick={() => setSelected(null)} style={{ display: "flex", alignItems: "center", gap: 4, fontSize: 13, fontWeight: 500, color: C.amber, background: "none", border: "none", cursor: "pointer", fontFamily: ui, padding: 0 }}>
            <svg width="9" height="16" viewBox="0 0 9 16" fill="none"><path d="M8 1L1.5 8L8 15" stroke={C.amber} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
            Docs
          </button>
          <div style={{ display: "flex", alignItems: "center", gap: 5 }}>
            <Dot color={isReview ? C.amber : C.green} size={5} />
            <span style={{ fontSize: 10, fontWeight: 600, color: isReview ? C.amber : C.green }}>{isReview ? "Needs review" : "Confirmed"}</span>
          </div>
        </div>

        <div style={{ padding: "0 14px", display: "flex", flexDirection: "column", gap: 12, paddingBottom: 24 }}>

          {/* Status banner */}
          <div style={{ padding: "10px 14px", borderRadius: 10, background: isReview ? C.amberWhisper : C.greenSoft, border: `1px solid ${isReview ? C.borderAmber : "rgba(30,132,73,0.12)"}` }}>
            <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 2 }}>
              <Dot color={isReview ? C.amber : C.green} pulse={isReview} size={6} />
              <span style={{ fontSize: 12, fontWeight: 700, color: C.text }}>{isReview ? "Review required" : "Confirmed"}</span>
            </div>
            <div style={{ fontSize: 10, color: C.textMuted }}>{isReview ? "AI extracted. Verify details below." : `Locked · ${doc.date}`}</div>
          </div>

          {/* Mini document preview card */}
          <Card style={{ overflow: "hidden" }}>
            <div style={{ padding: "16px 18px", background: "#fff", position: "relative" }}>
              <div style={{ position: "absolute", top: 10, right: 12, fontSize: 7, fontWeight: 600, fontFamily: num, color: dd.origin === "PEPPOL" ? C.amber : C.textMuted, background: dd.origin === "PEPPOL" ? C.amberWhisper : C.canvas, border: `1px solid ${dd.origin === "PEPPOL" ? C.borderAmber : C.border}`, borderRadius: 3, padding: "2px 5px" }}>{dd.origin}</div>
              <div style={{ fontSize: 16, fontWeight: 700, color: C.text, letterSpacing: "-0.02em" }}>{dd.pageTitle}</div>
              <div style={{ fontSize: 9, color: C.textMuted, marginBottom: 14 }}>{dd.pageSub}</div>
              <div style={{ display: "flex", gap: 16, marginBottom: 12, fontSize: 9.5, color: C.textSec }}>
                <div><div style={{ fontSize: 7, fontWeight: 600, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 2 }}>From</div><div style={{ fontWeight: 600, color: C.text, fontSize: 10 }}>{dd.vendor || "Unknown"}</div></div>
                <div><div style={{ fontSize: 7, fontWeight: 600, color: C.textMuted, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 2 }}>To</div><div style={{ fontWeight: 600, color: C.text, fontSize: 10 }}>INVOID VISION</div></div>
              </div>
              {/* Line items */}
              {dd.lines.map((line, li) => (
                <div key={li} style={{ display: "flex", justifyContent: "space-between", padding: "6px 0", borderTop: `1px solid ${C.border}`, fontSize: 10 }}>
                  <span style={{ color: C.text }}>{line.desc}</span>
                  <span style={{ fontFamily: num, color: C.text, flexShrink: 0, marginLeft: 8 }}>€{line.amt.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
                </div>
              ))}
              <div style={{ display: "flex", justifyContent: "space-between", padding: "8px 0 0", marginTop: 4, borderTop: `1.5px solid ${C.text}`, fontSize: 12, fontWeight: 700 }}>
                <span>Total</span>
                <span style={{ fontFamily: num }}>€{dd.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
              </div>
            </div>
          </Card>

          {/* Contact */}
          <Card>
            <div style={{ padding: "12px 16px" }}>
              <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 8 }}>Contact</div>
              {dd.needsContact ? (
                <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "8px 10px", background: C.amberWhisper, border: `1px solid ${C.borderAmber}`, borderRadius: 8 }}>
                  <Dot color={C.amber} size={6} />
                  <div style={{ flex: 1 }}><div style={{ fontSize: 11, fontWeight: 600, color: C.text }}>Who issued this?</div><div style={{ fontSize: 9, color: C.textMuted }}>Tap to identify</div></div>
                  <span style={{ fontSize: 12, color: C.textFaint }}>›</span>
                </div>
              ) : (
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <div style={{ width: 30, height: 30, borderRadius: 8, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 9, fontWeight: 700, fontFamily: num, color: C.amber }}>{initials}</div>
                  <div>
                    <div style={{ fontSize: 12, fontWeight: 600, color: C.text }}>{dd.vendor}</div>
                    {doc.ref?.startsWith("peppol") && <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>via PEPPOL</div>}
                  </div>
                </div>
              )}
            </div>
          </Card>

          {/* Cashflow — review only */}
          {isReview && (
            <Card style={{ padding: "12px 16px" }}>
              <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 8 }}>Cashflow</div>
              <div style={{ display: "flex", gap: 2, background: C.canvas, borderRadius: 7, padding: 2 }}>
                {["In", "Out"].map(d => (
                  <button key={d} style={{ flex: 1, padding: "7px 0", fontSize: 11, fontWeight: d === "Out" ? 600 : 400, fontFamily: ui, borderRadius: 5, border: "none", cursor: "pointer", color: d === "Out" ? "#fff" : C.textMuted, background: d === "Out" ? C.amber : "transparent" }}>{d}</button>
                ))}
              </div>
            </Card>
          )}

          {/* Invoice Details */}
          <Card style={{ padding: "4px 16px" }}>
            <div style={{ padding: "10px 0 4px" }}><span style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em" }}>Invoice Details</span></div>
            {[
              { l: "Invoice Number", v: dd.ref, mono: true, c: dd.refConf, e: isReview },
              { l: "Issue Date", v: dd.issueDate, mono: true, c: dd.issueDateConf, e: isReview },
              { l: "Due Date", v: dd.dueDate, mono: true, c: dd.dueDateConf, e: isReview, w: !dd.dueDate },
            ].map((r, i, a) => (
              <div key={i} style={{ display: "flex", alignItems: "center", gap: 6, padding: "10px 0", borderBottom: i < a.length - 1 ? `1px solid ${C.border}` : "none" }}>
                <ConfDot level={r.c} />
                <span style={{ fontSize: 11, color: C.textMuted, flex: 1 }}>{r.l}</span>
                <span style={{
                  fontSize: 12, fontWeight: 500, fontFamily: r.mono ? num : ui,
                  color: r.w ? C.amber : r.v ? C.text : C.textFaint,
                  padding: r.e ? "3px 8px" : 0,
                  background: r.e ? C.canvas : "transparent",
                  border: r.e ? `1px solid ${r.w ? C.borderAmber : C.border}` : "none",
                  borderRadius: r.e ? 6 : 0,
                }}>{r.v || "—"}</span>
              </div>
            ))}
          </Card>

          {/* Amounts */}
          <Card style={{ padding: "4px 16px" }}>
            <div style={{ padding: "10px 0 4px" }}><span style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em" }}>Amounts</span></div>
            {[
              { l: "Subtotal", v: `€${dd.subtotal.toLocaleString("de-DE", { minimumFractionDigits: 2 })}` },
              { l: "VAT " + dd.vatRate, v: `€${dd.vat.toLocaleString("de-DE", { minimumFractionDigits: 2 })}` },
            ].map((r, i) => (
              <div key={i} style={{ display: "flex", justifyContent: "space-between", padding: "10px 0", borderBottom: `1px solid ${C.border}` }}>
                <span style={{ fontSize: 11, color: C.textMuted }}>{r.l}</span>
                <span style={{ fontSize: 12, fontFamily: num, fontWeight: 500, color: C.text }}>{r.v}</span>
              </div>
            ))}
            <div style={{ display: "flex", justifyContent: "space-between", padding: "10px 0 8px" }}>
              <span style={{ fontSize: 13, fontWeight: 700, color: C.text }}>Total</span>
              <span style={{ fontSize: 15, fontWeight: 700, fontFamily: num, color: C.text }}>€{dd.total.toLocaleString("de-DE", { minimumFractionDigits: 2 })}</span>
            </div>
          </Card>

          {/* Sources */}
          <Card style={{ padding: "12px 16px" }}>
            <div style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 8 }}>Sources</div>
            {dd.sources.map((s, si) => (
              <div key={si} style={{ display: "flex", alignItems: "center", gap: 8, padding: "5px 0" }}>
                <span style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: s.type === "PEPPOL" ? C.amber : C.textMuted, background: s.type === "PEPPOL" ? C.amberWhisper : C.canvas, border: `1px solid ${s.type === "PEPPOL" ? C.borderAmber : C.border}`, borderRadius: 3, padding: "2px 5px", width: 42, textAlign: "center", flexShrink: 0 }}>{s.type}</span>
                <span style={{ fontSize: 11, color: C.text, flex: 1, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{s.name}</span>
                <span style={{ fontSize: 9, fontFamily: num, color: C.textFaint, flexShrink: 0 }}>{s.date}</span>
              </div>
            ))}
            {dd.sources.length > 1 && (
              <div style={{ fontSize: 9, color: C.green, marginTop: 6, display: "flex", alignItems: "center", gap: 4 }}>
                <Dot color={C.green} size={4} /> Sources matched
              </div>
            )}
          </Card>

          {/* Transactions */}
          <Card style={{ padding: "12px 16px" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 8 }}>
              <span style={{ fontSize: 9, fontWeight: 600, color: C.thText, textTransform: "uppercase", letterSpacing: "0.08em" }}>Transactions</span>
              {!isReview && dd.transactions.length === 0 && (
                <span style={{ fontSize: 10, fontWeight: 600, color: C.amber }}>+ Record</span>
              )}
            </div>
            {dd.transactions.length === 0 ? (
              <div style={{ padding: "4px 0", fontSize: 10.5, color: C.textFaint, textAlign: "center" }}>
                {isReview ? "Confirm first to track payment" : "No payment recorded yet"}
              </div>
            ) : dd.transactions.map((t, ti) => (
              <div key={ti} style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 0" }}>
                <Dot color={C.green} size={5} />
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 11, color: C.text }}>{t.method}</div>
                  <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{t.ref}</div>
                </div>
                <div style={{ textAlign: "right" }}>
                  <div style={{ fontSize: 11, fontFamily: num, fontWeight: 600, color: C.green }}>Paid</div>
                  <div style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{t.date}</div>
                </div>
              </div>
            ))}
          </Card>

          {/* Actions */}
          {isReview ? (
            <div style={{ display: "flex", flexDirection: "column", gap: 8, marginTop: 4 }}>
              <button style={{ width: "100%", padding: "12px 0", fontSize: 13, fontWeight: 600, fontFamily: ui, color: "#fff", background: C.amber, border: "none", borderRadius: 10, cursor: "pointer", boxShadow: "0 2px 8px rgba(184,134,11,0.25)" }}>Confirm</button>
              <div style={{ display: "flex", gap: 8 }}>
                <button style={{ flex: 1, padding: "10px 0", fontSize: 11, fontWeight: 500, fontFamily: ui, color: C.textMuted, background: C.page, border: `1px solid ${C.border}`, borderRadius: 8, cursor: "pointer" }}>Flag issue</button>
                <button style={{ flex: 1, padding: "10px 0", fontSize: 11, fontWeight: 500, fontFamily: ui, color: C.red, background: C.page, border: `1px solid ${C.border}`, borderRadius: 8, cursor: "pointer" }}>Reject</button>
              </div>
            </div>
          ) : (
            <div style={{ textAlign: "center", padding: "8px 0" }}>
              <span style={{ fontSize: 10, color: C.textFaint, textDecoration: "underline", textDecorationColor: C.textFaint, textUnderlineOffset: 2 }}>Request amendment</span>
            </div>
          )}
        </div>
      </div>
    );
  }

  // ── LIST VIEW ──
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
      {/* Filter tabs */}
      <div style={{ display: "flex", gap: 6, marginBottom: 4 }}>
        {[
          { id: "all", label: "All", count: ALL_DOCS.length },
          { id: "attn", label: "Attention", count: ALL_DOCS.filter(d => d.st === "warn").length },
        ].map(t => (
          <span key={t.id} style={{ fontSize: 11, fontWeight: 500, color: t.id === "all" ? C.text : C.amber, padding: "4px 0", borderBottom: t.id === "all" ? `1.5px solid ${C.text}` : "none", marginRight: 12 }}>{t.label} <span style={{ fontSize: 9, fontFamily: num, color: C.textMuted }}>{t.count}</span></span>
        ))}
      </div>

      {/* Document cards */}
      {ALL_DOCS.map((d, i) => (
        <Card key={i} style={{ cursor: "pointer" }} accent={d.st === "warn"}>
          <div onClick={() => setSelected(i)} style={{ display: "flex", alignItems: "center", gap: 12, padding: "12px 14px" }}>
            <Dot color={d.st === "warn" ? C.amber : C.green} size={6} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: C.text }}>{d.v}</div>
              <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 2 }}>
                <span style={{ fontSize: 9, fontFamily: num, color: C.textMuted, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{d.ref}</span>
                <span style={{ fontSize: 8, fontWeight: 600, fontFamily: num, color: d.src === "PEPPOL" ? C.amber : C.textMuted, background: d.src === "PEPPOL" ? C.amberWhisper : C.canvas, border: `1px solid ${d.src === "PEPPOL" ? C.borderAmber : C.border}`, borderRadius: 3, padding: "1px 4px" }}>{d.src}</span>
              </div>
            </div>
            <div style={{ textAlign: "right", flexShrink: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600, fontFamily: num, color: C.text }}>€{Math.abs(d.amt).toLocaleString("de-DE", { minimumFractionDigits: 2 })}</div>
              <div style={{ fontSize: 9, color: C.textMuted, marginTop: 1 }}>{d.date}</div>
            </div>
            <span style={{ fontSize: 14, color: C.textFaint, marginLeft: 2 }}>›</span>
          </div>
        </Card>
      ))}
    </div>
  );
}


// ═══════════════════════════════════════
//  MOBILE PROFILE
// ═══════════════════════════════════════
function MobileProfileScreen() {
  const Row = ({ label, value, mono, chevron, destructive, last }) => (
    <div style={{
      display: "flex", alignItems: "center", padding: "13px 16px",
      borderBottom: last ? "none" : `1px solid ${C.border}`,
    }}>
      <span style={{ fontSize: 13, color: destructive ? C.red : C.text, fontWeight: destructive ? 500 : 400, flex: 1 }}>{label}</span>
      {value && <span style={{ fontSize: 12, fontFamily: mono ? num : ui, color: C.textMuted }}>{value}</span>}
      {chevron && <span style={{ fontSize: 14, color: C.textFaint, marginLeft: 8 }}>›</span>}
    </div>
  );

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      {/* Avatar + identity */}
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", textAlign: "center", marginBottom: 8 }}>
        <div style={{
          width: 68, height: 68, borderRadius: 20, marginBottom: 12,
          background: `linear-gradient(135deg, ${C.amberSoft}, rgba(184,134,11,0.15))`,
          border: `1.5px solid ${C.borderAmber}`,
          display: "flex", alignItems: "center", justifyContent: "center",
          fontSize: 22, fontWeight: 700, fontFamily: num, color: C.amber,
        }}>AK</div>
        <div style={{ fontSize: 18, fontWeight: 700, color: C.text, letterSpacing: "-0.02em" }}>Artem Kuznetsov</div>
        <div style={{ fontSize: 12, color: C.textMuted, marginTop: 2 }}>artem@invoid.vision</div>
        <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 10 }}>
          <span style={{ fontSize: 10, fontWeight: 600, fontFamily: num, color: C.amber, background: C.amberSoft, border: `1px solid ${C.borderAmber}`, borderRadius: 5, padding: "3px 10px" }}>Core</span>
          <span style={{ fontSize: 10, fontWeight: 600, fontFamily: num, color: C.amber, background: C.amberWhisper, border: `1px solid ${C.borderAmber}`, borderRadius: 5, padding: "3px 10px" }}>Owner</span>
        </div>
      </div>

      {/* Account */}
      <Card style={{ overflow: "hidden" }}>
        <Row label="Email" value="artem@invoid.vision" mono />
        <Row label="Name" value="Artem Kuznetsov" />
        <Row label="Email Verification" value={
          <span style={{ display: "flex", alignItems: "center", gap: 5 }}>
            <Dot color={C.amber} size={5} />
            <span style={{ color: C.amber, fontSize: 11, fontWeight: 500 }}>Not verified</span>
          </span>
        } />
        <Row label="Verify email" chevron last />
      </Card>

      {/* Security */}
      <Card style={{ overflow: "hidden" }}>
        <Row label="Change Password" chevron />
        <Row label="Active Sessions" value="1 device" chevron last />
      </Card>

      {/* Server */}
      <Card style={{ overflow: "hidden" }}>
        <div style={{ padding: "12px 16px 4px", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Label>Server</Label>
          <Dot color={C.green} pulse size={5} />
        </div>
        <Row label="Server" value="Dokus Server" />
        <Row label="URL" value="192.168.0.193:8000" mono />
        <Row label="Version" value="0.1.0" mono />
        <Row label="Change Server" chevron last />
        <div style={{ padding: "6px 16px 12px", display: "flex", justifyContent: "center" }}>
          <span style={{ fontSize: 11, color: C.amber, fontWeight: 500 }}>☁ Reset to Cloud</span>
        </div>
      </Card>

      {/* Danger */}
      <Card style={{ overflow: "hidden" }}>
        <div style={{ padding: "12px 16px 4px" }}><Label color={C.red}>Danger Zone</Label></div>
        <div style={{ padding: "0 16px 6px" }}><span style={{ fontSize: 11, color: C.textMuted }}>Deactivating removes access to all workspaces.</span></div>
        <Row label="Deactivate Account" destructive last />
      </Card>

      {/* Log out */}
      <Card style={{ overflow: "hidden" }}>
        <Row label="Log Out" destructive last />
      </Card>

      {/* Footer */}
      <div style={{ textAlign: "center", paddingBottom: 4, fontSize: 10, fontFamily: num, color: C.textFaint }}>
        Dokus v0.1.0 · Core
      </div>
    </div>
  );
}


function MobileShell({screen,setScreen,children}){
  const gb={background:"rgba(242,241,238,0.55)",backdropFilter:"blur(50px)",WebkitBackdropFilter:"blur(50px)"};
  return (
    <div style={{display:"flex",flexDirection:"column",height:"100%",background:C.bg,fontFamily:ui,color:C.text,position:"relative"}}>
      <AmbientBackground/>
      {/* Header */}
      <div style={{padding:"12px 16px",...gb,borderBottom:"1px solid rgba(255,255,255,0.4)",display:"flex",alignItems:"center",justifyContent:"space-between",flexShrink:0,position:"sticky",top:0,zIndex:10}}>
        <div style={{display:"flex",alignItems:"center",gap:8}}>
          <div style={{width:24,height:24,borderRadius:6,border:"1.5px solid rgba(0,0,0,0.08)",background:"rgba(255,255,255,0.5)",display:"flex",alignItems:"center",justifyContent:"center",fontSize:9,fontWeight:700,fontFamily:num,color:C.amber}}>[#]</div>
          <span style={{fontSize:17,fontWeight:700,letterSpacing:"-0.02em"}}>Dokus</span>
        </div>
        {/* Profile avatar */}
        <div onClick={()=>setScreen("profile")} style={{
          width:28,height:28,borderRadius:8,cursor:"pointer",
          background:screen==="profile"?`linear-gradient(135deg, ${C.amberSoft}, rgba(184,134,11,0.2))`:"rgba(255,255,255,0.5)",
          border:`1.5px solid ${screen==="profile"?C.borderAmber:"rgba(0,0,0,0.08)"}`,
          display:"flex",alignItems:"center",justifyContent:"center",
          fontSize:9,fontWeight:700,fontFamily:num,color:screen==="profile"?C.amber:C.textMuted,transition:"all 0.15s",
        }}>AK</div>
      </div>
      {/* Content */}
      <div style={{flex:1,overflow:"auto",padding:"16px 14px 86px",position:"relative",zIndex:1}}>
        <h1 style={{fontSize:22,fontWeight:700,margin:"0 0 16px",letterSpacing:"-0.03em",textTransform:"capitalize"}}>{screen.replace("-"," ")}</h1>
        {children}
      </div>
      {/* Bottom bar */}
      <div style={{position:"fixed",bottom:0,left:0,right:0,...gb,borderTop:"1px solid rgba(255,255,255,0.4)",display:"flex",justifyContent:"space-around",padding:"8px 0 calc(8px + env(safe-area-inset-bottom,0px))",zIndex:10}}>
        {[{id:"today",label:"Today"},{id:"documents",label:"Docs"},{id:"cashflow",label:"Cash"},{id:"contacts",label:"Contacts"},{id:"accountant",label:"Export"}].map(item=>
          <button key={item.id} onClick={()=>setScreen(item.id)} style={{display:"flex",flexDirection:"column",alignItems:"center",gap:3,background:"none",border:"none",cursor:"pointer",padding:"4px 8px",fontFamily:ui,position:"relative"}}>
            {screen===item.id&&<div style={{position:"absolute",top:-1,left:"50%",transform:"translateX(-50%)",width:16,height:2,borderRadius:1,background:C.amber}}/>}
            <span style={{fontSize:10,fontWeight:screen===item.id?600:400,color:screen===item.id?C.text:C.textMuted}}>{item.label}</span>
          </button>
        )}
      </div>
    </div>
  );

}


// ═══════════════════════════════════════
//  MAIN
// ═══════════════════════════════════════
export default function DokusV14() {
  const [screen,setScreen]=useState("today");
  const [viewport,setViewport]=useState("desktop");
  const getContent=(mobile)=>{
    switch(screen){
      case "today": return <TodayScreen />;
      case "documents": return mobile ? <MobileDocumentsScreen /> : <DocumentsScreen />;
      case "cashflow": return <CashflowScreen />;
      case "accountant": return <AccountantScreen />;
      case "contacts": return mobile ? <MobileContactsScreen /> : <ContactsScreen />;
      case "profile": return mobile ? <MobileProfileScreen /> : <ProfileScreen />;
      default: return <TodayScreen />;
    }
  };
  return (
    <div style={{height:"100vh",display:"flex",flexDirection:"column",fontFamily:ui,background:"#e8e7e4"}}>
      <style>{CSS}</style>
      <div style={{background:"#fff",padding:"6px 20px",display:"flex",alignItems:"center",justifyContent:"space-between",flexShrink:0,borderBottom:`1px solid ${C.border}`}}>
        <span style={{fontSize:11,fontWeight:700,fontFamily:num,color:C.amber}}>D# v14</span>
        <div style={{display:"flex",gap:2,background:C.canvas,borderRadius:6,padding:2,border:`1px solid ${C.border}`}}>
          {["desktop","mobile"].map(v=><button key={v} onClick={()=>setViewport(v)} style={{fontSize:11,fontWeight:viewport===v?600:400,color:viewport===v?C.text:C.textMuted,background:viewport===v?"#fff":"transparent",boxShadow:viewport===v?C.shadow:"none",border:"none",borderRadius:5,padding:"4px 14px",cursor:"pointer",fontFamily:ui,textTransform:"capitalize"}}>{v}</button>)}
        </div>
      </div>
      <div style={{flex:1,display:"flex",justifyContent:"center",alignItems:viewport==="mobile"?"center":"stretch",background:"#e8e7e4",padding:viewport==="mobile"?24:0,overflow:"hidden"}}>
        {viewport==="mobile" ? (
          <div style={{width:390,height:844,borderRadius:28,overflow:"hidden",border:`1px solid rgba(0,0,0,0.08)`,boxShadow:"0 40px 100px rgba(0,0,0,0.12)"}}><MobileShell screen={screen} setScreen={setScreen}>{getContent(true)}</MobileShell></div>
        ) : (
          <div style={{width:"100%",height:"100%"}}><DesktopShell screen={screen} setScreen={setScreen}/></div>
        )}
      </div>
    </div>
  );
}
