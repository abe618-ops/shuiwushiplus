const questions = [
  {id:'T1-VAT-001',subject:'tax1',session:['dawn','noon'],chapter:'第二章 增值税',level:'掌握',type:'单选',stem:'依据税法学习中的第一性原理，判断一项交易是否进入增值税分析时，最应优先确认的是哪一组基础要素？',options:['纳税主体、交易性质与征税范围','企业规模、员工人数与利润率','会计科目、报表格式与审计意见','行业排名、市场份额与品牌价值'],answer:0,explanation:'增值税分析首先要回到底层法律结构：谁发生了什么交易、该交易是否属于征税范围，再继续判断税率、计税方法、优惠与纳税义务发生时间。',source:'原创仿真题；范围依据：2025年度《税法（Ⅰ）》考试大纲'},
  {id:'T1-VAT-002',subject:'tax1',session:['morning','evening'],chapter:'第二章 增值税',level:'掌握',type:'辨析',stem:'某考生能背出增值税税率，但遇到材料题时经常把“征税范围判断”和“税率选择”混在一起。最有效的改进方式是哪一种？',options:['继续反复抄写税率表','先按交易性质判断是否征税，再独立判断税率与计税方法','只做同一类型题直到形成肌肉记忆','跳过征税范围，直接从应纳税额公式开始'],answer:1,explanation:'这是典型的知识结构混淆。应把“是否进入税制”与“进入后如何计税”分层，形成稳定的判断链。',source:'原创仿真题；学习策略题'},
  {id:'T1-CT-001',subject:'tax1',session:['noon','evening'],chapter:'第三章 消费税',level:'掌握',type:'单选',stem:'学习消费税时，把从价定率、从量定额、复合计征混在一起训练，主要体现了哪一种学习方法？',options:['交错练习','单纯重复阅读','被动识记','延迟反馈'],answer:0,explanation:'交错练习通过混合相似但不同的规则，迫使学习者辨认条件和适用边界，特别适合税种计算方法的区分。',source:'原创仿真题；范围依据：2025年度《税法（Ⅰ）》考试大纲'},
  {id:'T1-PRINCIPLE-001',subject:'tax1',session:['morning'],chapter:'第一章 税法基本原理',level:'掌握',type:'单选',stem:'按照官方大纲的能力层级，“掌握”最接近下列哪种表现？',options:['只需要知道概念名称','能够在较复杂职业环境中综合运用知识处理涉税问题','只需要记住一条定义','能够复述教材目录即可'],answer:1,explanation:'官方大纲将要求区分为了解、熟悉、掌握，其中“掌握”强调综合运用知识处理相对复杂涉税实务问题。',source:'原创仿真题；依据2025年度《税法（Ⅰ）》考试大纲'},
  {id:'T2-CIT-001',subject:'tax2',session:['dawn','morning','noon'],chapter:'企业所得税',level:'理解',type:'单选',stem:'用第一性原理学习企业所得税计算时，最合理的起点是什么？',options:['先背所有优惠政策','先明确纳税主体、所得范围及应纳税所得额形成逻辑','先背历年答案字母','先研究考试时间分配'],answer:1,explanation:'企业所得税的复杂计算应先建立“主体—所得—税基—调整—优惠—税额”的骨架，再向其中填充具体规则。',source:'原创仿真题；学习结构题'},
  {id:'T2-METHOD-001',subject:'tax2',session:['evening'],chapter:'综合训练',level:'迁移',type:'材料策略',stem:'一道综合题同时涉及企业所得税收入确认、扣除项目和税收优惠。以下哪种作答流程更稳健？',options:['看到优惠就先套优惠','先列事实，再按收入→扣除→调整→优惠→税额的顺序处理','直接心算最终税额','只检查最后一个数字'],answer:1,explanation:'复杂题需要把事实拆解后按税基形成顺序推进，能显著减少漏项和顺序错误。',source:'原创仿真题；学习结构题'}
];

const sessionPlans={
  dawn:[['清晨记忆','5–8分钟','低负荷：概念、数字、规则边界'],['快速回忆','3–5分钟','不看答案先说出核心规则']],
  morning:[['新知拆解','15–20分钟','第一性原理理解新知识'],['费曼复述','5分钟','用自己的话讲明白规则']],
  noon:[['主动回忆','10–15分钟','选择、辨析与错题复现'],['交错小测','5–10分钟','混合相似规则']],
  evening:[['综合训练','20–30分钟','计算、材料、跨知识点'],['错因复盘','10分钟','记录错误类型与下次复习']]
};

let current=[],idx=0,selected=null,done=0;
const $=id=>document.getElementById(id);

function renderPlan(session){
  $('planList').innerHTML=sessionPlans[session].map(([a,b,c])=>`<div class="plan-item"><b>${a}</b><small>${b} · ${c}</small></div>`).join('');
}
function pool(){
  const s=$('subjectSelect').value, session=$('sessionSelect').value;
  return questions.filter(q=>(s==='all'||q.subject===s)&&q.session.includes(session));
}
function generate(){
  current=pool(); idx=0; done=0; selected=null;
  renderPlan($('sessionSelect').value); updateProgress(); renderQuestion();
}
function renderQuestion(){
  const q=current[idx];
  $('explanation').classList.add('hidden');
  $('submitBtn').disabled=true; $('nextBtn').disabled=true; selected=null;
  if(!q){$('qTitle').textContent='当前组合暂无样题';$('qStem').textContent='切换科目或时段即可继续。后续联网题库会自动补齐。';$('options').innerHTML='';$('qIndex').textContent='0 / 0';return;}
  $('qMeta').textContent=`${q.source.includes('原创')?'原创仿真题':'核验题'} · ${q.level}`;
  $('qTitle').textContent=`${q.subject==='tax1'?'税法一':'税法二'} · ${q.chapter}`;
  $('qStem').textContent=q.stem; $('qIndex').textContent=`${idx+1} / ${current.length}`;
  $('options').innerHTML=q.options.map((x,i)=>`<div class="option" data-i="${i}">${String.fromCharCode(65+i)}. ${x}</div>`).join('');
  document.querySelectorAll('.option').forEach(el=>el.onclick=()=>{document.querySelectorAll('.option').forEach(x=>x.classList.remove('selected'));el.classList.add('selected');selected=Number(el.dataset.i);$('submitBtn').disabled=false;});
}
function submit(){
  const q=current[idx]; if(selected===null||!q)return;
  document.querySelectorAll('.option').forEach((el,i)=>{if(i===q.answer)el.classList.add('correct');if(i===selected&&i!==q.answer)el.classList.add('wrong');el.onclick=null;});
  $('explanation').innerHTML=`<b>${selected===q.answer?'回答正确':'回答错误'}</b><br>${q.explanation}<br><small>来源标识：${q.source}</small>`;
  $('explanation').classList.remove('hidden'); $('submitBtn').disabled=true; $('nextBtn').disabled=false;
  done=Math.max(done,idx+1); updateProgress();
}
function next(){if(idx<current.length-1){idx++;renderQuestion()}else{$('qTitle').textContent='本时段任务完成';$('qStem').textContent='可以切换时段继续，或回看错题。';$('options').innerHTML='';$('explanation').classList.add('hidden');$('nextBtn').disabled=true;}}
function updateProgress(){const p=current.length?Math.round(done/current.length*100):0;$('progressText').textContent=p+'%';$('progressBar').style.width=p+'%';}
$('generateBtn').onclick=generate;$('submitBtn').onclick=submit;$('nextBtn').onclick=next;$('sessionSelect').onchange=()=>renderPlan($('sessionSelect').value);renderPlan('dawn');
