<template>
  <div class="page-shell">
    <header class="page-heading">
      <div>
        <p class="eyebrow">采购协作</p>
        <h1>询价与报价</h1>
        <p>仅显示采购方明确邀请贵司参与的询价。</p>
      </div>
      <a-button :loading="loading" @click="load"
        ><ReloadOutlined /> 刷新</a-button
      >
    </header>
    <div class="filter-row">
      <a-segmented v-model:value="filter" :options="filterOptions" /><span
        >{{ filtered.length }} 条询价</span
      >
    </div>
    <a-skeleton v-if="loading" active />
    <a-empty v-else-if="filtered.length === 0" description="暂无询价邀请" />
    <div v-else class="inquiry-list">
      <article v-for="item in filtered" :key="item.id" class="inquiry-card">
        <div class="inquiry-main">
          <div class="inquiry-title-row">
            <a-tag color="blue">{{ item.code }}</a-tag
            ><a-tag :color="quoteStatus(item).color">{{
              quoteStatus(item).text
            }}</a-tag>
          </div>
          <h2>{{ item.title }}</h2>
          <p>
            {{ item.lines.length }} 项物料 · 邀请于
            {{ formatDate(item.invitedAt) }}
          </p>
          <p v-if="item.awardStatus === 'AWARDED'" class="award-summary">
            中标日期 {{ formatDate(item.awardedAt || "") }} ·
            {{
              item.contract
                ? `采购订单（合同）${item.contract.contractNo} ${contractStatusText(item.contract.status)}`
                : "采购方尚未下单"
            }}
          </p>
          <div class="line-preview">
            <span v-for="line in item.lines.slice(0, 3)" :key="line.requestId"
              >{{ line.partName || line.requestCode }} ×
              {{ line.quantity }}</span
            ><span v-if="item.lines.length > 3"
              >等 {{ item.lines.length }} 项</span
            >
          </div>
        </div>
        <div class="inquiry-progress">
          <div
            class="countdown"
            :class="{ urgent: daysLeft(item.deadline) <= 2 }"
          >
            <ClockCircleOutlined /><strong>{{
              deadlineText(item.deadline)
            }}</strong
            ><small>{{ item.deadline || "采购方未设置截止日期" }}</small>
          </div>
          <a-progress
            :percent="completion(item)"
            size="small"
            :show-info="false"
          />
          <small>报价完整度 {{ completion(item) }}%</small>
        </div>
        <div class="inquiry-action">
          <a-button type="primary" @click="openEditor(item)"
            >{{ item.quote ? "查看 / 编辑报价" : "开始报价" }} <RightOutlined
          /></a-button>
        </div>
      </article>
    </div>

    <a-drawer
      v-model:open="editorOpen"
      class="quote-drawer"
      width="min(920px, 100vw)"
      :closable="false"
    >
      <template #title
        ><div class="drawer-title">
          <a-button type="text" aria-label="关闭" @click="editorOpen = false"
            ><CloseOutlined
          /></a-button>
          <div>
            <strong>{{ selected?.title }}</strong
            ><span>{{ selected?.code }}</span>
          </div>
        </div></template
      >
      <template #extra
        ><a-tag v-if="selected" :color="quoteStatus(selected).color">{{
          quoteStatus(selected).text
        }}</a-tag></template
      >
      <template v-if="selected">
        <div class="quote-deadline">
          <ClockCircleOutlined />
          <div>
            <strong>{{ deadlineText(selected.deadline) }}</strong
            ><span>截止日期 {{ selected.deadline || "未设置" }}</span>
          </div>
          <a-progress :percent="completionFromForm" :show-info="false" />
        </div>
        <a-alert
          v-if="selected.awardStatus === 'AWARDED'"
          type="success"
          show-icon
          message="恭喜，贵司已中标"
          :description="
            selected.contract
              ? `采购订单（合同）${selected.contract.contractNo} · ${money(selected.contract.amount)} · ${contractStatusText(selected.contract.status)}`
              : '采购方下单后生成采购订单（合同），请关注后续状态。'
          "
          class="quote-alert"
        />
        <a-alert
          v-else-if="selected.awardStatus === 'NOT_AWARDED'"
          type="info"
          show-icon
          message="本次询价未中标"
          description="报价及沟通记录仍保留供贵司查阅。"
          class="quote-alert"
        />
        <section
          v-if="selected.awardStatus === 'AWARDED' && selected.contract"
          class="quote-section"
        >
          <div class="section-title">
            <div>
              <h2>采购订单（合同）附件</h2>
              <p>采购方上传的合同及随附文件，可下载留存。</p>
            </div>
            <a-tag v-if="selected.contract.acknowledged" color="green"
              >已确认{{
                selected.contract.acknowledgedByName
                  ? " · " + selected.contract.acknowledgedByName
                  : ""
              }}</a-tag
            >
            <a-button
              v-else
              type="primary"
              size="small"
              :loading="acknowledging"
              @click="acknowledge"
              ><CheckOutlined /> 确认合同</a-button
            >
          </div>
          <a-list
            v-if="selected.contract.documents?.length"
            size="small"
            :data-source="selected.contract.documents"
          >
            <template #renderItem="{ item: doc }"
              ><a-list-item
                ><span>{{ doc.fileName }} · {{ fileSize(doc.sizeBytes) }}</span
                ><a-button
                  type="link"
                  size="small"
                  @click="downloadContractDocument(doc)"
                  ><DownloadOutlined /> 下载</a-button
                ></a-list-item
              ></template
            > </a-list
          ><a-empty
            v-else
            :image="Empty.PRESENTED_IMAGE_SIMPLE"
            description="采购方尚未上传合同附件"
          />
          <a-divider style="margin: 12px 0" />
          <div class="section-title">
            <div>
              <h2>发货回传</h2>
              <p>发货后回传送货单号与预计到货，采购方可在订单中查看。</p>
            </div>
          </div>
          <a-form layout="inline" class="shipment-form">
            <a-form-item label="送货单号"
              ><a-input
                v-model:value="shipmentForm.deliveryNo"
                placeholder="例如：SF1234567890"
            /></a-form-item>
            <a-form-item label="承运方"
              ><a-input
                v-model:value="shipmentForm.carrier"
                placeholder="例如：顺丰"
            /></a-form-item>
            <a-form-item label="预计到货"
              ><a-input
                v-model:value="shipmentForm.expectedArrival"
                type="date"
            /></a-form-item>
            <a-form-item label="备注"
              ><a-input v-model:value="shipmentForm.remark" placeholder="可选"
            /></a-form-item>
            <a-form-item
              ><a-button
                type="primary"
                :loading="shipmentSaving"
                @click="submitShipment"
                >回传发货</a-button
              ></a-form-item
            >
          </a-form>
          <a-alert
            v-if="shipmentSent"
            type="success"
            show-icon
            message="发货信息已回传，采购方可提前安排收货。"
            class="quote-alert"
          />
        </section>
        <a-alert
          v-if="
            selected.quote?.source === 'INTERNAL_ENTRY' &&
            !selected.quote.confirmed
          "
          type="info"
          show-icon
          message="这份报价由采购员代录入"
          description="请核对价格、税率、交期及条款，确认后会留下贵司账号与确认时间。"
          class="quote-alert"
        />
        <a-alert
          v-if="!store.canQuote"
          type="warning"
          show-icon
          message="当前暂不能提交报价"
          description="账号与供应商准入均审核通过后，报价操作才会开放。"
          class="quote-alert"
        />
        <a-alert
          v-else-if="selectedExpired"
          type="warning"
          show-icon
          message="该询价已截止"
          description="报价内容仍可查看，但不能再保存、提交、撤回或确认。"
          class="quote-alert"
        />
        <section class="quote-section">
          <div class="section-title">
            <div>
              <h2>分项报价</h2>
              <p>所有物料均需填写含税单价与税率。</p>
            </div>
            <span
              >{{ completedLines }}/{{ quoteForm.lines.length }} 已完成</span
            >
          </div>
          <div class="quote-table-wrap">
            <table class="quote-table">
              <thead>
                <tr>
                  <th>物料 / 数量</th>
                  <th>含税单价</th>
                  <th>税率</th>
                  <th>交付日期</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="line in quoteForm.lines" :key="line.requestId">
                  <td>
                    <strong>{{ line.partName || line.requestCode }}</strong
                    ><small
                      >{{ line.requestCode }} · 数量 {{ line.quantity }}</small
                    >
                    <small v-if="line.historicalPrice" class="hist-price"
                      >历史成交均价 {{ money(line.historicalPrice) }}</small
                    >
                  </td>
                  <td>
                    <a-input-number
                      v-model:value="line.unitPrice"
                      :min="0.01"
                      :precision="2"
                      :disabled="readOnly"
                      addon-before="¥"
                    />
                  </td>
                  <td>
                    <a-input-number
                      v-model:value="line.taxRate"
                      :min="0"
                      :max="100"
                      :disabled="readOnly"
                      addon-after="%"
                    />
                  </td>
                  <td>
                    <a-input
                      v-model:value="line.deliveryDate"
                      type="date"
                      :disabled="readOnly"
                    />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
        <section class="quote-section">
          <div class="section-title">
            <div>
              <h2>商务条款</h2>
              <p>补充报价有效期、付款与其他费用。</p>
            </div>
          </div>
          <div class="form-grid-3">
            <a-form-item label="运费"
              ><a-input-number
                v-model:value="quoteForm.freightAmount"
                :min="0"
                :disabled="readOnly"
                addon-before="¥" /></a-form-item
            ><a-form-item label="其他费用"
              ><a-input-number
                v-model:value="quoteForm.otherCostAmount"
                :min="0"
                :disabled="readOnly"
                addon-before="¥" /></a-form-item
            ><a-form-item label="报价有效期"
              ><a-input
                v-model:value="quoteForm.validUntil"
                type="date"
                :disabled="readOnly"
            /></a-form-item>
          </div>
          <a-form-item label="付款条件"
            ><a-input
              v-model:value="quoteForm.paymentTerms"
              :disabled="readOnly"
              placeholder="例如：验收后 30 天付款"
          /></a-form-item>
          <a-form-item label="报价说明"
            ><a-textarea
              v-model:value="quoteForm.remark"
              :rows="3"
              :disabled="readOnly"
          /></a-form-item>
        </section>
        <section class="quote-total">
          <span
            >报价合计<small
              >物料 {{ money(materialAmount) }} + 其他费用</small
            ></span
          ><strong>{{ money(totalAmount) }}</strong>
        </section>
        <section class="quote-section">
          <div class="section-title">
            <div>
              <h2>报价附件</h2>
              <p>可补充盖章报价单、技术方案或其他说明。</p>
            </div>
            <a-tooltip :title="!selected.quote ? '请先保存报价草稿' : ''"
              ><a-upload
                :show-upload-list="false"
                :before-upload="uploadAttachment"
                :disabled="
                  !canOperate ||
                  !selected.quote ||
                  selected.quote.status !== 'DRAFT'
                "
                ><a-button
                  size="small"
                  :disabled="
                    !canOperate ||
                    !selected.quote ||
                    selected.quote.status !== 'DRAFT'
                  "
                  ><PaperClipOutlined /> 上传附件</a-button
                ></a-upload
              ></a-tooltip
            >
          </div>
          <a-list
            v-if="selected.attachments?.length"
            size="small"
            :data-source="selected.attachments"
          >
            <template #renderItem="{ item: attachment }"
              ><a-list-item
                ><span
                  >{{ attachment.fileName }} ·
                  {{ fileSize(attachment.sizeBytes) }}</span
                ><a-space
                  ><a-button
                    type="link"
                    size="small"
                    @click="downloadAttachment(attachment)"
                    ><DownloadOutlined /> 下载</a-button
                  ><a-popconfirm
                    v-if="selected.quote?.status === 'DRAFT' && canOperate"
                    title="删除此附件？"
                    @confirm="removeAttachment(attachment)"
                    ><a-button type="link" danger size="small"
                      >删除</a-button
                    ></a-popconfirm
                  ></a-space
                ></a-list-item
              ></template
            > </a-list
          ><a-empty
            v-else
            :image="Empty.PRESENTED_IMAGE_SIMPLE"
            description="暂无报价附件"
          />
        </section>
        <section class="quote-section">
          <div class="section-title">
            <div>
              <h2>询价澄清</h2>
              <p>提交问题后由采购方统一回复并留痕。</p>
            </div>
          </div>
          <a-space-compact style="width: 100%"
            ><a-input
              v-model:value="clarificationQuestion"
              placeholder="例如：交付地点和包装要求是否有特殊约定？"
            /><a-button
              type="primary"
              :disabled="!canOperate || !clarificationQuestion.trim()"
              @click="askQuestion"
              >提问</a-button
            ></a-space-compact
          >
          <a-list
            v-if="selected.clarifications?.length"
            size="small"
            :data-source="selected.clarifications"
            style="margin-top: 12px"
            ><template #renderItem="{ item: clarification }"
              ><a-list-item
                ><div>
                  <strong>{{ clarification.question }}</strong>
                  <p v-if="clarification.answer" class="table-subtitle">
                    {{ clarification.answer }} ·
                    {{ clarification.answeredByName }}
                  </p>
                  <p v-else class="table-subtitle">等待采购方回复</p>
                </div></a-list-item
              ></template
            ></a-list
          >
        </section>
      </template>
      <template #footer>
        <div class="drawer-footer">
          <a-button
            v-if="
              selected?.quote?.status === 'SUBMITTED' &&
              selected.quote.source === 'SUPPLIER_PORTAL'
            "
            danger
            :disabled="!canOperate"
            :loading="saving"
            @click="withdraw"
            >撤回报价</a-button
          >
          <a-button
            v-if="
              selected &&
              !selected.quote &&
              selected.invitationStatus !== 'DECLINED'
            "
            danger
            type="link"
            :disabled="!canOperate"
            @click="decline"
            >放弃本次报价</a-button
          >
          <span class="footer-spacer" />
          <a-button @click="editorOpen = false">关闭</a-button>
          <a-button
            v-if="
              selected?.quote?.source === 'INTERNAL_ENTRY' &&
              !selected.quote.confirmed
            "
            type="primary"
            :disabled="!canOperate"
            :loading="saving"
            @click="confirm"
            ><CheckOutlined /> 确认代录报价</a-button
          >
          <template v-else-if="!readOnly"
            ><a-button
              :disabled="!canOperate"
              :loading="saving"
              @click="persist(false)"
              ><SaveOutlined /> 保存草稿</a-button
            ><a-popconfirm
              title="提交后如需修改，需先撤回报价。确认提交？"
              @confirm="persist(true)"
              ><a-button type="primary" :disabled="!canOperate"
                ><SendOutlined /> 提交报价</a-button
              ></a-popconfirm
            ></template
          >
        </div>
      </template>
    </a-drawer>
    <a-modal
      v-model:open="declineOpen"
      title="放弃本次报价"
      ok-text="确认放弃"
      cancel-text="取消"
      :ok-button-props="{ danger: true, disabled: !declineReason.trim() }"
      :confirm-loading="saving"
      @ok="submitDecline"
    >
      <a-alert
        type="warning"
        show-icon
        message="放弃后采购方会看到原因；如需恢复，请联系采购方重新邀请。"
        style="margin-bottom: 16px"
      />
      <a-form layout="vertical"
        ><a-form-item label="放弃原因" required
          ><a-textarea
            v-model:value="declineReason"
            :rows="4"
            :maxlength="500"
            show-count
            placeholder="例如：当前产能不足，无法满足交付周期" /></a-form-item
      ></a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { message } from "ant-design-vue";
import {
  CheckOutlined,
  ClockCircleOutlined,
  CloseOutlined,
  DownloadOutlined,
  PaperClipOutlined,
  ReloadOutlined,
  RightOutlined,
  SaveOutlined,
  SendOutlined,
} from "@ant-design/icons-vue";
import * as api from "../api";
import { usePortalStore } from "../store";
import { Empty } from "ant-design-vue";

type EditableLine = api.QuoteLine & { unitPrice: number; taxRate: number };
const store = usePortalStore();
const loading = ref(false);
const saving = ref(false);
const editorOpen = ref(false);
const filter = ref("OPEN");
const selected = ref<api.PortalInquiry>();
const inquiries = ref<api.PortalInquiry[]>([]);
const quoteForm = reactive({
  lines: [] as EditableLine[],
  paymentTerms: "",
  remark: "",
  currency: "CNY",
  freightAmount: 0,
  otherCostAmount: 0,
  validUntil: "",
});
const clarificationQuestion = ref("");
const declineOpen = ref(false);
const declineReason = ref("");
const acknowledging = ref(false);
const shipmentSaving = ref(false);
const shipmentSent = ref(false);
const shipmentForm = reactive({
  deliveryNo: "",
  carrier: "",
  expectedArrival: "",
  remark: "",
});
const filterOptions = [
  { label: "进行中", value: "OPEN" },
  { label: "待报价", value: "PENDING" },
  { label: "已提交", value: "SUBMITTED" },
  { label: "已中标", value: "AWARDED" },
  { label: "全部", value: "ALL" },
];
const needsAction = (item: api.PortalInquiry) =>
  !item.quote ||
  item.quote.status !== "SUBMITTED" ||
  (item.quote.source === "INTERNAL_ENTRY" && !item.quote.confirmed);
const filtered = computed(() =>
  inquiries.value.filter(
    (i) =>
      filter.value === "ALL" ||
      (filter.value === "OPEN" && i.status === "OPEN") ||
      (filter.value === "PENDING" && i.status === "OPEN" && needsAction(i)) ||
      (filter.value === "SUBMITTED" &&
        i.quote?.status === "SUBMITTED" &&
        !needsAction(i)) ||
      (filter.value === "AWARDED" && i.awardStatus === "AWARDED"),
  ),
);
const completedLines = computed(
  () =>
    quoteForm.lines.filter(
      (l) => Number(l.unitPrice) > 0 && Number(l.taxRate) >= 0,
    ).length,
);
const completionFromForm = computed(() =>
  quoteForm.lines.length
    ? Math.round((completedLines.value / quoteForm.lines.length) * 100)
    : 0,
);
const materialAmount = computed(() =>
  quoteForm.lines.reduce(
    (sum, l) => sum + Number(l.quantity || 0) * Number(l.unitPrice || 0),
    0,
  ),
);
const totalAmount = computed(
  () =>
    materialAmount.value +
    Number(quoteForm.freightAmount || 0) +
    Number(quoteForm.otherCostAmount || 0),
);
const readOnly = computed(
  () =>
    selected.value?.quote?.status === "SUBMITTED" ||
    selected.value?.quote?.source === "INTERNAL_ENTRY",
);
const selectedExpired = computed(
  () =>
    !selected.value ||
    selected.value.status !== "OPEN" ||
    daysLeft(selected.value.deadline) < 0,
);
const canOperate = computed(() => store.canQuote && !selectedExpired.value);
onMounted(load);
async function load() {
  loading.value = true;
  try {
    inquiries.value = await api.listInquiries();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "加载失败");
  } finally {
    loading.value = false;
  }
}
function openEditor(item: api.PortalInquiry) {
  selected.value = item;
  clarificationQuestion.value = "";
  const quoteLines = new Map(
    (item.quote?.lines || []).map((l) => [l.requestId, l]),
  );
  quoteForm.lines = item.lines.map((l) => ({
    ...l,
    ...quoteLines.get(l.requestId),
    unitPrice: Number(quoteLines.get(l.requestId)?.unitPrice || 0),
    taxRate: Number(quoteLines.get(l.requestId)?.taxRate ?? 13),
    deliveryDate: String(
      quoteLines.get(l.requestId)?.deliveryDate || l.expectedDate || "",
    ),
  }));
  Object.assign(quoteForm, {
    paymentTerms: item.quote?.paymentTerms || "",
    remark: item.quote?.remark || "",
    currency: item.quote?.currency || "CNY",
    freightAmount: Number(item.quote?.freightAmount || 0),
    otherCostAmount: Number(item.quote?.otherCostAmount || 0),
    validUntil: item.quote?.validUntil || "",
  });
  editorOpen.value = true;
}
function payload() {
  return {
    ...quoteForm,
    lines: quoteForm.lines.map(
      ({ requestId, unitPrice, taxRate, deliveryDate, remark }) => ({
        requestId,
        unitPrice: Number(unitPrice),
        taxRate: Number(taxRate),
        deliveryDate: deliveryDate || undefined,
        remark,
      }),
    ),
  };
}
async function persist(submit: boolean) {
  if (completionFromForm.value < 100 || !selected.value) {
    message.warning("请完整填写所有分项报价");
    return;
  }
  saving.value = true;
  try {
    const inquiryId = selected.value.id;
    await api.saveQuote(inquiryId, payload(), submit);
    message.success(submit ? "报价已提交" : "草稿已保存，可继续上传附件");
    if (submit) editorOpen.value = false;
    await load();
    if (!submit) {
      const refreshed = inquiries.value.find((item) => item.id === inquiryId);
      if (refreshed) selected.value = refreshed;
    }
  } catch (e) {
    message.error(e instanceof Error ? e.message : "保存失败");
  } finally {
    saving.value = false;
  }
}
async function withdraw() {
  if (!selected.value) return;
  saving.value = true;
  try {
    await api.withdrawQuote(selected.value.id);
    message.success("报价已撤回，可继续修改");
    editorOpen.value = false;
    await load();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "撤回失败");
  } finally {
    saving.value = false;
  }
}
async function confirm() {
  if (!selected.value) return;
  saving.value = true;
  try {
    await api.confirmQuote(selected.value.id);
    message.success("已确认采购员代录报价");
    editorOpen.value = false;
    await load();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "确认失败");
  } finally {
    saving.value = false;
  }
}
function decline() {
  declineReason.value = "";
  declineOpen.value = true;
}
async function submitDecline() {
  if (!selected.value || !declineReason.value.trim()) return;
  saving.value = true;
  try {
    await api.declineInquiry(selected.value.id, declineReason.value.trim());
    message.success("已记录放弃报价");
    declineOpen.value = false;
    editorOpen.value = false;
    await load();
  } catch (e) {
    message.error(e instanceof Error ? e.message : "操作失败");
  } finally {
    saving.value = false;
  }
}
async function uploadAttachment(file: File) {
  if (!selected.value) return false;
  const data = new FormData();
  data.append("attachmentType", "QUOTATION");
  data.append("file", file);
  try {
    await api.uploadQuoteAttachment(selected.value.id, data);
    message.success("附件已上传");
    await load();
    const refreshed = inquiries.value.find(
      (item) => item.id === selected.value?.id,
    );
    if (refreshed) selected.value = refreshed;
  } catch (e) {
    message.error(e instanceof Error ? e.message : "上传失败");
  }
  return false;
}
async function askQuestion() {
  if (!selected.value || !clarificationQuestion.value.trim()) return;
  try {
    await api.askClarification(
      selected.value.id,
      clarificationQuestion.value.trim(),
    );
    clarificationQuestion.value = "";
    await load();
    const refreshed = inquiries.value.find(
      (item) => item.id === selected.value?.id,
    );
    if (refreshed) selected.value = refreshed;
    message.success("问题已提交");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "提交失败");
  }
}
async function downloadAttachment(attachment: api.QuoteAttachment) {
  if (!selected.value) return;
  const response = await fetch(
    api.quoteAttachmentDownloadUrl(selected.value.id, attachment.id),
    {
      headers: {
        Authorization: `Bearer ${localStorage.getItem(api.SUPPLIER_TOKEN_KEY) || ""}`,
      },
    },
  );
  if (!response.ok) return message.error("下载失败");
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement("a");
  link.href = url;
  link.download = attachment.fileName;
  link.click();
  URL.revokeObjectURL(url);
}
async function downloadContractDocument(doc: api.ContractDocument) {
  const response = await fetch(api.contractDocumentDownloadUrl(doc.id), {
    headers: {
      Authorization: `Bearer ${localStorage.getItem(api.SUPPLIER_TOKEN_KEY) || ""}`,
    },
  });
  if (!response.ok) return message.error("下载失败");
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement("a");
  link.href = url;
  link.download = doc.fileName;
  link.click();
  URL.revokeObjectURL(url);
}
async function acknowledge() {
  if (!selected.value?.contract) return;
  acknowledging.value = true;
  try {
    await api.acknowledgeContract(selected.value.contract.id);
    message.success("已确认采购订单（合同）");
    await load();
    const refreshed = inquiries.value.find(
      (item) => item.id === selected.value?.id,
    );
    if (refreshed) selected.value = refreshed;
  } catch (e) {
    message.error(e instanceof Error ? e.message : "确认失败");
  } finally {
    acknowledging.value = false;
  }
}
async function submitShipment() {
  if (!selected.value?.contract?.orderId) return;
  if (!shipmentForm.deliveryNo.trim()) {
    message.warning("请填写送货单号");
    return;
  }
  shipmentSaving.value = true;
  try {
    await api.createShipment(selected.value.contract.orderId, {
      deliveryNo: shipmentForm.deliveryNo.trim(),
      carrier: shipmentForm.carrier.trim() || undefined,
      expectedArrival: shipmentForm.expectedArrival || undefined,
      remark: shipmentForm.remark.trim() || undefined,
    });
    message.success("发货信息已回传");
    shipmentSent.value = true;
    shipmentForm.deliveryNo = "";
    shipmentForm.carrier = "";
    shipmentForm.expectedArrival = "";
    shipmentForm.remark = "";
  } catch (e) {
    message.error(e instanceof Error ? e.message : "回传失败");
  } finally {
    shipmentSaving.value = false;
  }
}
async function removeAttachment(attachment: api.QuoteAttachment) {
  if (!selected.value) return;
  const inquiryId = selected.value.id;
  try {
    await api.deleteQuoteAttachment(inquiryId, attachment.id);
    await load();
    const refreshed = inquiries.value.find((item) => item.id === inquiryId);
    if (refreshed) selected.value = refreshed;
    message.success("附件已删除");
  } catch (e) {
    message.error(e instanceof Error ? e.message : "删除失败");
  }
}
function completion(item: api.PortalInquiry) {
  const lines = item.quote?.lines || [];
  return item.lines.length
    ? Math.round(
        (lines.filter((l) => Number(l.unitPrice) > 0).length /
          item.lines.length) *
          100,
      )
    : 0;
}
function daysLeft(deadline?: string) {
  return deadline
    ? Math.ceil(
        (new Date(`${deadline}T23:59:59`).getTime() - Date.now()) / 86400000,
      )
    : 999;
}
function deadlineText(deadline?: string) {
  const d = daysLeft(deadline);
  return !deadline
    ? "未设截止"
    : d < 0
      ? "已截止"
      : d === 0
        ? "今天截止"
        : `${d} 天后截止`;
}
function quoteStatus(item: api.PortalInquiry) {
  if (item.awardStatus === "AWARDED") return { color: "green", text: "已中标" };
  if (item.awardStatus === "NOT_AWARDED")
    return { color: "default", text: "未中标" };
  if (item.quote?.source === "INTERNAL_ENTRY" && !item.quote.confirmed)
    return { color: "orange", text: "采购代录 · 待确认" };
  if (item.quote?.status === "SUBMITTED")
    return { color: "blue", text: "已提交 · 待定标" };
  if (item.quote?.status === "DRAFT" || item.quote?.status === "WITHDRAWN")
    return { color: "orange", text: "草稿" };
  return { color: "default", text: "待报价" };
}
function contractStatusText(value: string) {
  return (
    (
      {
        DRAFT: "随订单待生效",
        PENDING_APPROVAL: "审批中",
        ACTIVE: "已生效",
        REJECTED: "已驳回",
        SUPERSEDED: "已变更",
      } as Record<string, string>
    )[value] || value
  );
}
const money = (v: number) =>
  new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(
    Number(v || 0),
  );
const formatDate = (v: string) =>
  new Intl.DateTimeFormat("zh-CN").format(new Date(v));
const fileSize = (v: number) =>
  v > 1048576 ? `${(v / 1048576).toFixed(1)} MB` : `${Math.ceil(v / 1024)} KB`;
</script>
