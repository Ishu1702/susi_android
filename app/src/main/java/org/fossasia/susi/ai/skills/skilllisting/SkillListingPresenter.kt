package org.fossasia.susi.ai.skills.skilllisting

import org.fossasia.susi.ai.R
import org.fossasia.susi.ai.data.SkillListingModel
import org.fossasia.susi.ai.data.UtilModel
import org.fossasia.susi.ai.data.contract.ISkillListingModel
import org.fossasia.susi.ai.dataclasses.SkillMetricsDataQuery
import org.fossasia.susi.ai.dataclasses.SkillsBasedOnMetrics
import org.fossasia.susi.ai.helper.Constant
import org.fossasia.susi.ai.helper.PrefManager
import org.fossasia.susi.ai.rest.responses.susi.*
import org.fossasia.susi.ai.skills.skilllisting.contract.ISkillListingPresenter
import org.fossasia.susi.ai.skills.skilllisting.contract.ISkillListingView
import retrofit2.Response
import timber.log.Timber

/**
 * Skill Listing Presenter
 *
 * Created by chiragw15 on 15/8/17.
 */
class SkillListingPresenter(val skillListingFragment: SkillListingFragment) : ISkillListingPresenter, ISkillListingModel.OnFetchGroupsFinishedListener,
        ISkillListingModel.OnFetchSkillsFinishedListener, ISkillListingModel.OnFetchSkillMetricsFinishedListener {

    private var skillListingModel: ISkillListingModel = SkillListingModel()
    private var skillListingView: ISkillListingView? = null
    private var count = 1
    private var skills: ArrayList<Pair<String, List<SkillData>>> = ArrayList()
    private var metrics = SkillsBasedOnMetrics(ArrayList(), ArrayList(), ArrayList())
    private var metricsData: Metrics? = null
    private var groupsCount = 0
    private val utilModel = UtilModel(skillListingFragment.requireContext())

    override fun onAttach(skillListingView: ISkillListingView) {
        this.skillListingView = skillListingView
    }

    override fun getGroups(swipeToRefreshActive: Boolean) {
        skillListingView?.visibilityProgressBar(!swipeToRefreshActive)
        skillListingModel.fetchGroups(this)
    }

    override fun getMetrics(swipeToRefreshActive: Boolean) {
        skillListingView?.visibilityProgressBar(!swipeToRefreshActive)
        val queryObject = SkillMetricsDataQuery("general", PrefManager.getString(Constant.LANGUAGE, Constant.DEFAULT))
        skillListingModel.fetchSkillMetrics(queryObject, this)
    }

    override fun onGroupFetchSuccess(response: Response<ListGroupsResponse>) {
        if (response.isSuccessful && response.body() != null) {
            Timber.d("GROUPS FETCHED")
            groupsCount = response.body().groups.size
            metrics.groups = response.body().groups as MutableList<String>
            if (metrics.groups != null) {
                skillListingView?.updateAdapter(metrics)
            }
        } else {
            Timber.d("GROUPS NOT FETCHED")
            skillListingView?.visibilityProgressBar(false)
            skillListingView?.displayError()
        }
    }

    override fun onGroupFetchFailure(t: Throwable) {
        skillListingView?.visibilityProgressBar(false)
        skillListingView?.displayError()
    }

    override fun onSkillFetchSuccess(response: Response<ListSkillsResponse>, group: String) {
        skillListingView?.visibilityProgressBar(false)
        if (response.isSuccessful && response.body() != null) {
            Timber.d("SKILLS FETCHED")
            val responseSkillMap = response.body().filteredSkillsData
            if (responseSkillMap.isNotEmpty()) {
                skills.add(Pair(group, responseSkillMap))
            }
            if (count != groupsCount) {
                skillListingModel.fetchSkills(metrics.groups[count], PrefManager.getString(Constant.LANGUAGE, Constant.DEFAULT), this)
                count++
            }
        } else {
            Timber.d("SKILLS NOT FETCHED")
            skillListingView?.visibilityProgressBar(false)
            skillListingView?.displayError()
        }
    }

    override fun onSkillFetchFailure(t: Throwable) {
        skillListingView?.visibilityProgressBar(false)
        skillListingView?.displayError()
    }

    override fun onSkillMetricsFetchSuccess(response: Response<ListSkillMetricsResponse>) {
        skillListingView?.visibilityProgressBar(false)
        if (response.isSuccessful && response.body() != null) {
            Timber.d("METRICS FETCHED")
            metricsData = response.body().metrics
            if (metricsData != null) {
                metrics.metricsList.clear()
                metrics.metricsGroupTitles.clear()
                if (metricsData?.rating != null) {
                    if (metricsData?.rating?.size as Int > 0) {
                        metrics.metricsGroupTitles.add(utilModel.getString(R.string.metric_rating))
                        metrics.metricsList.add(metricsData?.rating)
                        skillListingView?.updateAdapter(metrics)
                    }
                }

                if (metricsData?.usage != null) {
                    if (metricsData?.usage?.size as Int > 0) {
                        metrics.metricsGroupTitles.add(utilModel.getString(R.string.metric_usage))
                        metrics.metricsList.add(metricsData?.usage)
                        skillListingView?.updateAdapter(metrics)
                    }
                }

                if (metricsData?.newest != null) {
                    val size = metricsData?.newest?.size
                    if (size is Int) {
                        if (size > 0) {
                            metrics.metricsGroupTitles.add(utilModel.getString(R.string.metric_newest))
                            metrics.metricsList.add(metricsData?.newest)
                            skillListingView?.updateAdapter(metrics)
                        }
                    }
                }

                if (metricsData?.latest != null) {
                    if (metricsData?.latest?.size as Int > 0) {
                        metrics.metricsGroupTitles.add(utilModel.getString(R.string.metric_latest))
                        metrics.metricsList.add(metricsData?.latest)
                        skillListingView?.updateAdapter(metrics)
                    }
                }

                if (metricsData?.feedback != null) {
                    if (metricsData?.feedback?.size as Int > 0) {
                        metrics.metricsGroupTitles.add(utilModel.getString(R.string.metric_feedback))
                        metrics.metricsList.add(metricsData?.feedback)
                        skillListingView?.updateAdapter(metrics)
                    }
                }

                if (metricsData?.topGames != null) {
                    val size = metricsData?.feedback?.size
                    if (size is Int) {
                        if (size > 0) {
                            metrics.metricsGroupTitles.add(utilModel.getString(R.string.metrics_top_games))
                            metrics.metricsList.add(metricsData?.topGames)
                            skillListingView?.updateAdapter(metrics)
                        }
                    }
                }

                skillListingModel.fetchGroups(this)
            }
        } else {
            Timber.d("METRICS NOT FETCHED")
            skillListingView?.visibilityProgressBar(false)
            skillListingView?.displayError()
        }
    }

    override fun onSkillMetricsFetchFailure(t: Throwable) {
        skillListingView?.visibilityProgressBar(false)
        skillListingView?.displayError()
    }

    override fun onDetach() {
        skillListingModel.cancelFetch()
        skillListingView = null
    }
}
